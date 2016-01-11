package com.mikerussellnz.taiwanwifi.Clustering;

import java.util.ArrayList;

/**
 * Created by mike on 6/01/16.
 */
public class QuadTreeNode<T extends QuadTreeItem> {
	public static final int MAX_NODE_CAPACITY = 32;

	private BoundingBox _boundingBox;

	private QuadTreeNode<T> _northEast;
	private QuadTreeNode<T> _northWest;
	private QuadTreeNode<T> _southEast;
	private QuadTreeNode<T> _southWest;

	private ArrayList<T> _items;

	public QuadTreeNode(BoundingBox boundingBox) {
		_boundingBox = boundingBox;
	}

	public boolean isLeaf() {
		return _northEast == null;
	}

	private void subdivide() {
		BoundingBox box = _boundingBox;
		double xMid = (box.x2 + box.x1) / 2.0;
		double yMid = (box.y2 + box.y1) / 2.0;

		_northEast = new QuadTreeNode(new BoundingBox(xMid, box.y1, box.x2, yMid));
		_northWest = new QuadTreeNode(new BoundingBox(box.x1, box.y1, xMid, yMid));
		_southEast = new QuadTreeNode(new BoundingBox(xMid, yMid, box.x2, box.y2));
		_southWest = new QuadTreeNode(new BoundingBox(box.x1, yMid, xMid, box.y2));

		for (T item : _items) {
			if (_northEast.insertItem(item)) {
				continue;
			}
			if (_northWest.insertItem(item)) {
				continue;
			}
			if (_southEast.insertItem(item)) {
				continue;
			}
			if (_southWest.insertItem(item)) {
				continue;
			}
			throw new RuntimeException("Impossible to get here.");
		}
		_items.clear();
	}

	public boolean insertItem(T item) {
		if (!_boundingBox.contains(item.getLocation())) {
			return false;
		}

		if (isLeaf() && count() < QuadTreeNode.MAX_NODE_CAPACITY) {
			addItem(item);
			return true;
		}

		if (isLeaf()) {
			subdivide();
		}

		if (_northEast.insertItem(item)) return true;
		if (_northWest.insertItem(item)) return true;
		if (_southEast.insertItem(item)) return true;
		if (_southWest.insertItem(item)) return true;

		throw new RuntimeException("Impossible to get here.");
	}

	private int count() {
		return _items != null ? _items.size() : 0;
	}

	private void addItem(T item) {
		if (_items == null) {
			_items = new ArrayList<>(MAX_NODE_CAPACITY);
		}
		_items.add(item);
	}

	public ArrayList<T> query(BoundingBox boundingBox) {
		ArrayList<T> results = new ArrayList<>();
		query(boundingBox, results);
		return results;
	}

	private void query(BoundingBox boundingBox, ArrayList<T> results) {
		if (!_boundingBox.intersects(boundingBox)) {
			return;
		}

		if (isLeaf()) {
			if (_items != null) {
				for (T item : _items) {
					if (boundingBox.contains(item.getLocation())) {
						results.add(item);
					}
				}
			}
			return;
		}

		_northEast.query(boundingBox, results);
		_northWest.query(boundingBox, results);
		_southEast.query(boundingBox, results);
		_southWest.query(boundingBox, results);
	}
}
