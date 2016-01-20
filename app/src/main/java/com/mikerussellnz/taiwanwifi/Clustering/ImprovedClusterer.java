package com.mikerussellnz.taiwanwifi.Clustering;

/**
 * Created by mike on 20/01/16.
 */
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mike on 6/01/16.
 */
public class ImprovedClusterer<T extends Marker> extends Clusterer<T> {
	protected final float GRIDSIZE = 75 * DisplayModel.getDeviceScaleFactor();

	public ImprovedClusterer(MapView mapView) {
		super(mapView);
	}

	@Override
	public boolean requiresItemsQuadTree() {
		return true;
	}

	@Override
	public Collection<Cluster<T>> cluster() {
		final Set<QuadTreeWrapper<T>> visited = new HashSet<>();
		final Set<Cluster<T>> clusters = new HashSet<>();
		final Map<QuadTreeWrapper<T>, Double> distanceToCurrentCluster = new HashMap<>();
		final Map<QuadTreeWrapper<T>, Cluster<T>> currentCluster = new HashMap<>();

		int gridSize = (int)Math.round(GRIDSIZE * getCurrentZoomScaleFactor());

		for (QuadTreeWrapper<T> candidate : _mItems) {
			if (visited.contains(candidate)) {
				continue;
			}

			Point pos = candidate.getLocation();
			BoundingBox searchBounds = new BoundingBox(pos.x - gridSize, pos.y - gridSize, pos.x + gridSize, pos.y + gridSize);

			Collection<QuadTreeWrapper<T>> queryResults= _itemsTree.query(searchBounds);
			if (queryResults.size() == 1) {
				Cluster<T> c = new Cluster<>(candidate.getItem());
				c.setCachedPoint(candidate.getLocation());
				clusters.add(c);
				visited.add(candidate);
				distanceToCurrentCluster.put(candidate, 0.0);
				continue;
			}

			Cluster<T> cluster = new Cluster<>(candidate.getItem());
			cluster.setCachedPoint(candidate.getLocation());
			clusters.add(cluster);

			for (QuadTreeWrapper<T> clusterItem : queryResults) {
				Double existingDistance = distanceToCurrentCluster.get(clusterItem);
				double distance = clusterItem.getLocation().distance(candidate.getLocation());
				if (existingDistance != null) {
					if (existingDistance < distance) {
						continue;
					}
					currentCluster.get(clusterItem).remove(clusterItem.getItem());
				}
				distanceToCurrentCluster.put(clusterItem, distance);
				cluster.addItem(clusterItem.getItem());
				currentCluster.put(clusterItem, cluster);
			}
			visited.addAll(queryResults);
		}

		return clusters;
	}
}
