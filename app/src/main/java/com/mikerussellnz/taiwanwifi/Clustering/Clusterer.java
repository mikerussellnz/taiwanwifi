package com.mikerussellnz.taiwanwifi.Clustering;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 6/01/16.
 */
public class Clusterer<T extends Marker> {
	protected final float GRIDSIZE = 75 * DisplayModel.getDeviceScaleFactor();
	protected int _maxClusteringZoom = 16;

	protected MapView _mapView;

	protected List<Cluster<T>> _clusters = new ArrayList<>();

	public Clusterer(MapView mapView) {
		_mapView = mapView;
	}

	public List<Cluster<T>> getClusters() {
		return _clusters;
	}

	private Point toPixels(long mapSize, LatLong in) {
		return new Point(
				(int) (MercatorProjection.longitudeToPixelX(in.longitude, mapSize)),
				(int) (MercatorProjection.latitudeToPixelY(in.latitude, mapSize)));
	}

	public void addAll(ArrayList<T> items) {
		System.out.println("clustering");
		Model model = _mapView.getModel();

		if (_maxClusteringZoom >= model.mapViewPosition.getZoomLevel()) {
			MapPosition mapPosition = model.mapViewPosition.getMapPosition();
			long mapSize = MercatorProjection.getMapSize(mapPosition.zoomLevel, model.displayModel.getTileSize());

			QuadTreeNode<Cluster> quadTree = new QuadTreeNode<>(new BoundingBox(0, 0, mapSize, mapSize));

			synchronized (_clusters) {
				for (T item: items) {
					Point pos = toPixels(mapSize, item.getLatLong());

					BoundingBox queryBox = new BoundingBox(pos.x - GRIDSIZE, pos.y - GRIDSIZE, pos.x + GRIDSIZE, pos.y + GRIDSIZE);
					ArrayList<Cluster> potentials = quadTree.query(queryBox);

					// check existing clusters...
					double closestDist = GRIDSIZE;
					Cluster<T> closest = null;

					for (Cluster<T> cluster : potentials) {
						Point ptCenter = cluster.getCachedPoint();
						// check to see if the cluster contains the marker.
						double distance = pos.distance(ptCenter);
						if (distance <= GRIDSIZE) {
							if (closest == null) {
								closest = cluster;
								closestDist = distance;
							} else {
								if (distance < closestDist) {
									closestDist = distance;
									closest = cluster;
								} else if (distance == closestDist) {
									Point centerPixelsA = toPixels(mapSize, closest.getCenter());
									Point centerPixelsB = toPixels(mapSize, cluster.getCenter());
									double distanceA = pos.distance(centerPixelsA);
									double distanceB = pos.distance(centerPixelsB);

									if (distanceB < distanceA) {
										closest = cluster;
										closestDist = distanceB;
									}
								}
							}
						}
					}

					if (closest != null) {
						closest.addItem(item);
					} else {
						// No cluster contain the marker, create a new cluster.
						Cluster<T> cluster = new Cluster<>(item);
						Point ptCenter = toPixels(mapSize, item.getLatLong());
						cluster.setCachedPoint(ptCenter);
						_clusters.add(cluster);
						quadTree.insertItem(cluster);
					}
				}
			}
		} else {
			// No clustering allowed, create a new cluster with single item.
			synchronized (_clusters) {
				for (T item: items) {
					_clusters.add(new Cluster<>(item));
				}
			}
		}
		System.out.println("clustering done.");
	}
}
