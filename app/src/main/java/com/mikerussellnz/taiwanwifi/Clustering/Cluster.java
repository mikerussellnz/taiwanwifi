package com.mikerussellnz.taiwanwifi.Clustering;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.overlay.Marker;

import java.util.ArrayList;

/**
 * Created by mike on 6/01/16.
 */
public class Cluster<T extends Marker> implements QuadTreeItem {
	private ArrayList<T> _items;
	private LatLong _center;
	private Point _cachedPoint;

	public Cluster(T item) {
		_items = new ArrayList<>();
		_items.add(item);
	}

	public ArrayList<T> getItems() {
		return _items;
	}

	public void addItem(T item) {
		_items.add(item);
		_center = null;
	}

	public T getAnchorItem() {
		return _items.get(0);
	}

	public LatLong getCenter() {
		if (_center != null) {
			return _center;
		}
		// computing the centroid
		double lat = 0, lon = 0;
		int n = 0;
		synchronized (_items) {
			for (T object : _items) {
				if (object == null) {
					throw new NullPointerException("object == null");
				}
				if (object.getLatLong() == null) {
					throw new NullPointerException("object.getLatLong() == null");
				}
				lat += object.getLatLong().latitude;
				lon += object.getLatLong().longitude;
				n++;
			}
		}
		_center = new LatLong(lat / n, lon / n);
		return _center;
	}

	public void setCachedPoint(Point cachedPoint) {
		_cachedPoint = cachedPoint;
	}

	public Point getCachedPoint() {
		return _cachedPoint;
	}

	@Override
	public Point getLocation() {
		return _cachedPoint;
	}

	public void remove(T item) {
		_items.remove(item);
		_center = null;
	}
}
