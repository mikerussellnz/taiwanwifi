package com.mikerussellnz.taiwanwifi.Clustering;

import org.mapsforge.core.model.Point;

/**
 * Created by mike on 20/01/16.
 */
public class QuadTreeWrapper<T> implements QuadTreeItem {
	private Point _point;
	private T _item;

	public QuadTreeWrapper(T item, Point point) {
		_item = item;
		_point = point;
	}

	public T getItem() {
		return _item;
	}

	@Override
	public Point getLocation() {
		return _point;
	}
}
