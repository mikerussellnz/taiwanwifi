package com.mikerussellnz.taiwanwifi.Clustering;

import org.mapsforge.core.model.Point;

/**
 * Created by mike on 6/01/16.
 */
public class BoundingBox {
	public double x1;
	public double y1;
	public double x2;
	public double y2;

	public BoundingBox(double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public BoundingBox(Point p1, Point p2) {
		this.x1 = p1.x;
		this.y1 = p1.y;
		this.x2 = p2.x;
		this.y2 = p2.y;
	}

	public boolean contains(Point p) {
		boolean containsX = x1 <= p.x && p.x <= x2;
		boolean containsY = y1 <= p.y && p.y <= y2;
		return containsX && containsY;
	}

	public boolean intersects(BoundingBox box2) {
		return (x1 <= box2.x2 && x2 >= box2.x1 && y1 <= box2.y2 && y2 >= box2.y1);
	}

	public double absoluteWidth() {
		return Math.abs(x1 - x2);
	}

	public double absoluteHeight() {
		return Math.abs(y1 - y2);
	}

	public BoundingBox expandBy(double expansion) {
		return new BoundingBox(x1 - expansion, y1 - expansion, x2 + expansion, y2 + expansion);
	}
}
