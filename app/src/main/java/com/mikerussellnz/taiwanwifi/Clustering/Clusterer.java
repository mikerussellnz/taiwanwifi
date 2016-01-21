package com.mikerussellnz.taiwanwifi.Clustering;

import android.app.Activity;
import android.util.DisplayMetrics;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.util.MapPositionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by mike on 20/01/16.
 */
public abstract class Clusterer<T extends Marker> implements Observer {
	protected MapView _mapView;
	protected ArrayList<T> _items = new ArrayList<>();
	protected ArrayList<QuadTreeWrapper<T>> _mItems = new ArrayList<>();
	protected QuadTreeNode<QuadTreeWrapper<T>> _itemsTree;
	protected long _mapSizePixels;

	private Activity _activity;

	private double _oldZoomLevel = -1.0;

	private BoundingBox _worldBounds;

	protected QuadTreeNode<Cluster<T>> _clustersTree;
	private ArrayList<Cluster<T>> _clusters = new ArrayList<>();

	private Map<Cluster<T>, Layer> _currentLayerLookup = new HashMap<>();
	private BoundingBox _currentVisibleClusterQuery;

	public Clusterer(Activity activity, MapView mapView) {
		_activity = activity;
		_mapView = mapView;
		MapViewPosition position = _mapView.getModel().mapViewPosition;
		position.addObserver(this);
		_mapSizePixels = getMapSizePixelsAtZoom(20);
		_worldBounds = new BoundingBox(0, 0, _mapSizePixels, _mapSizePixels);
		_itemsTree = new QuadTreeNode<>(_worldBounds);
	}

	protected long getMapSizePixelsAtZoom(int zoomLevel) {
		return MercatorProjection.getMapSize((byte) zoomLevel, _mapView.getModel().displayModel.getTileSize());
	}

	protected double getCurrentZoomScaleFactor() {
		long mapSizeAtCurrentZoom = getMapSizePixelsAtZoom(_mapView.getModel().mapViewPosition.getZoomLevel());
		double scaleFactor = _mapSizePixels / mapSizeAtCurrentZoom;
		return scaleFactor;
	}

	protected Point locationToPixels(LatLong in) {
		return new Point(
				(int) (MercatorProjection.longitudeToPixelX(in.longitude, _mapSizePixels)),
				(int) (MercatorProjection.latitudeToPixelY(in.latitude, _mapSizePixels)));
	}

	public void addAll(ArrayList<T> items) {
		_items.addAll(items);

		if (requiresItemsQuadTree()) {
			for (T item : items) {
				Point pos = locationToPixels(item.getLatLong());
				QuadTreeWrapper<T> mqti = new QuadTreeWrapper<>(item, pos);
				_mItems.add(mqti);
				_itemsTree.insertItem(mqti);
			}
		}

		reclusterAndUpdateMap();
	}

	public abstract boolean requiresItemsQuadTree();

	protected abstract Collection<Cluster<T>> cluster();

	protected void recluster() {
		_clusters.clear();
		_clustersTree = new QuadTreeNode<>(_worldBounds);
		_clusters.addAll(cluster());
		_clustersTree.insertItems(_clusters);
		onRecluster(_clusters);
	}

	protected BoundingBox getPixelBoundingBox(org.mapsforge.core.model.BoundingBox mapBoundingBox, double expansionDivisor) {
		Point tl = locationToPixels(new LatLong(mapBoundingBox.maxLatitude, mapBoundingBox.minLongitude));
		Point br = locationToPixels(new LatLong(mapBoundingBox.minLatitude, mapBoundingBox.maxLongitude));

		BoundingBox boundingBox = new BoundingBox(
				tl,
				br);

		double expansion = Math.max(boundingBox.absoluteWidth(), boundingBox.absoluteHeight()) / expansionDivisor;
		boundingBox = boundingBox.expandBy(expansion);
		return boundingBox;
	}

	protected Collection<Cluster<T>> getClustersForBounds(BoundingBox boundingBox) {
		return _clustersTree.query(boundingBox);
	}

	protected abstract Layer getDisplayLayerSingleItem(T item);

	protected abstract Layer getDisplayLayerCluster(Cluster<T> cluster);

	protected abstract void addLayersToMap(Collection<Layer> layers);

	protected abstract void removeLayersFromMap(Collection<Layer> layers);

	protected void onRecluster(ArrayList<Cluster<T>> newClusters) {
	}

	@Override
	public void onChange() {
		boolean onMainThread = Thread.currentThread() == _activity.getMainLooper().getThread();

		// we are not interested in the zoomanimator changes, we only recluster when the zoom is complete.
		// luckily, it fires a notifyObservers on the main thread ad the end of the zoom animation.
		if (onMainThread) {
			int zoomLevel = _mapView.getModel().mapViewPosition.getZoomLevel();
			if (zoomLevel != _oldZoomLevel) {
				_oldZoomLevel = zoomLevel;
				reclusterAndUpdateMap();
				return;
			}
		}
		updateVisibleClusters();
	}

	private synchronized void updateVisibleClusters() {
		if (_currentVisibleClusterQuery == null) {
			return;
		}

		Model model = _mapView.getModel();

		org.mapsforge.core.model.BoundingBox mapBoundingBox = MapPositionUtil.getBoundingBox(
				model.mapViewPosition.getMapPosition(),
				getMapViewDimensionOrFallback(model),
				model.displayModel.getTileSize());

		BoundingBox requeryTest = getPixelBoundingBox(mapBoundingBox, 4);

		// does a 25% overscan of the screen still fit in our 50% overscan query box, if so
		// no need to requery clusters.
		if (_currentVisibleClusterQuery.contains(requeryTest)) {
			return;
		}

		BoundingBox clusterQueryBoundingBox = getPixelBoundingBox(mapBoundingBox, 2);

		HashSet<Cluster<T>> nc = new HashSet(getClustersForBounds(clusterQueryBoundingBox));
		HashSet<Cluster<T>> ec = new HashSet(_currentLayerLookup.keySet());

		HashSet<Cluster<T>> tr = new HashSet(ec);
		tr.removeAll(nc);

		HashSet<Cluster<T>> ta = new HashSet(nc);
		ta.removeAll(ec);

		ArrayList<Layer> layersToRemove = new ArrayList<>(tr.size());
		for (Cluster<T> cluster : tr) {
			Layer displayItem = _currentLayerLookup.get(cluster);
			layersToRemove.add(displayItem);
			_currentLayerLookup.remove(cluster);
		}

		ArrayList<Layer> layersToAdd = new ArrayList<>(ta.size());
		for (Cluster<T> cluster : ta) {
			Layer displayItem;
			if (cluster.getItems().size() == 1) {
				displayItem = getDisplayLayerSingleItem(cluster.getItems().get(0));
			} else {
				displayItem = getDisplayLayerCluster(cluster);
			}
			layersToAdd.add(displayItem);
			_currentLayerLookup.put(cluster, displayItem);
		}

		removeLayersFromMap(layersToRemove);
		addLayersToMap(layersToAdd);
		_currentVisibleClusterQuery = clusterQueryBoundingBox;
	}

	private Dimension getMapViewDimensionOrFallback(Model model) {
		Dimension dimension = model.mapViewDimension.getDimension();

		// fallback, if we don't have a dimension then the map hasn't been rendered.
		// assume the map takes the whole screen as an approximation.
		if (dimension == null) {
			DisplayMetrics displaymetrics = new DisplayMetrics();
			_activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			dimension = new Dimension(displaymetrics.widthPixels, displaymetrics.heightPixels);
		}

		return dimension;
	}

	private synchronized void reclusterAndUpdateMap() {
		recluster();

		removeLayersFromMap(_currentLayerLookup.values());
		_currentLayerLookup.clear();

		final Model model = _mapView.getModel();

		org.mapsforge.core.model.BoundingBox mapBoundingBox = MapPositionUtil.getBoundingBox(
					model.mapViewPosition.getMapPosition(),
					getMapViewDimensionOrFallback(model),
					model.displayModel.getTileSize());

		BoundingBox clusterQueryBoundingBox = getPixelBoundingBox(mapBoundingBox, 2);

		Collection<Cluster<T>> clusters = getClustersForBounds(clusterQueryBoundingBox);
		Collection<Layer> layersToAdd = new ArrayList<>();

		for (Cluster<T> cluster : clusters) {
			Layer displayItem;
			if (cluster.getItems().size() == 1) {
				displayItem = getDisplayLayerSingleItem(cluster.getItems().get(0));
			} else {
				displayItem = getDisplayLayerCluster(cluster);
			}
			layersToAdd.add(displayItem);
			_currentLayerLookup.put(cluster, displayItem);
		}

		addLayersToMap(layersToAdd);
		_currentVisibleClusterQuery = clusterQueryBoundingBox;
	}
}
