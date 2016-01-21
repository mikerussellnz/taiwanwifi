package com.mikerussellnz.taiwanwifi.Clustering;

import android.app.Activity;

import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.Model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 6/01/16.
 */
public abstract class NaiveClusterer<T extends Marker> extends Clusterer<T> {
	protected final float GRIDSIZE = 75 * DisplayModel.getDeviceScaleFactor();
	protected int _maxClusteringZoom = 16;

	public NaiveClusterer(Activity activity, MapView mapView) {
		super(activity, mapView);
	}

	@Override
	public boolean requiresItemsQuadTree() {
		return false;
	}

	@Override
	public Collection<Cluster<T>> cluster() {
		ArrayList<Cluster<T>> clusters = new ArrayList<>();
		Model model = _mapView.getModel();

		int gridSize = (int)Math.round(GRIDSIZE * getCurrentZoomScaleFactor());

		if (_maxClusteringZoom >= model.mapViewPosition.getZoomLevel()) {
			QuadTreeNode<Cluster> quadTree = new QuadTreeNode<>(new BoundingBox(0, 0, _mapSizePixels, _mapSizePixels));

				for (T item: _items) {
					Point pos = locationToPixels(item.getLatLong());

					BoundingBox queryBox = new BoundingBox(pos.x - gridSize, pos.y - gridSize, pos.x + gridSize, pos.y + gridSize);
					ArrayList<Cluster> potentials = quadTree.query(queryBox);

					// check existing clusters...
					double closestDist = gridSize;
					Cluster<T> closest = null;

					for (Cluster<T> cluster : potentials) {
						Point ptCenter = cluster.getCachedPoint();
						// check to see if the cluster contains the marker.
						double distance = pos.distance(ptCenter);
						if (distance <= gridSize) {
							if (closest == null) {
								closest = cluster;
								closestDist = distance;
							} else {
								if (distance < closestDist) {
									closestDist = distance;
									closest = cluster;
								} else if (distance == closestDist) {
									Point centerPixelsA = locationToPixels(closest.getCenter());
									Point centerPixelsB = locationToPixels(cluster.getCenter());
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
						Point ptCenter = locationToPixels(item.getLatLong());
						cluster.setCachedPoint(ptCenter);
						clusters.add(cluster);
						quadTree.insertItem(cluster);
					}
				}

		} else {
			// No clustering allowed, create a new cluster with single item.
			for (T item : _items) {
				clusters.add(new Cluster<>(item));
			}

		}
		return clusters;
	}
}
