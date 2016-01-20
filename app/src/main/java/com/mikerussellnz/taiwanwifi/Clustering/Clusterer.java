package com.mikerussellnz.taiwanwifi.Clustering;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Marker;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 20/01/16.
 */
public abstract class Clusterer<T extends Marker> {
	protected MapView _mapView;
	protected ArrayList<T> _items = new ArrayList<>();
	protected ArrayList<QuadTreeWrapper<T>> _mItems = new ArrayList<>();
	protected QuadTreeNode<QuadTreeWrapper<T>> _itemsTree;
	protected long _mapSizePixels;

	public Clusterer(MapView mapView) {
		_mapView = mapView;
		_mapSizePixels = getMapSizePixelsAtZoom(20);
		_itemsTree = new QuadTreeNode<>(new BoundingBox(0, 0, _mapSizePixels, _mapSizePixels));
	}

	protected long getMapSizePixelsAtZoom(int zoomLevel) {
		return MercatorProjection.getMapSize((byte)zoomLevel, _mapView.getModel().displayModel.getTileSize());
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
	}

	public abstract boolean requiresItemsQuadTree();

	public abstract Collection<Cluster<T>> cluster();
}
