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

	public boolean contains(Point p) {
		boolean containsX = x1 <= p.x && p.x <= x2;
		boolean containsY = y1 <= p.y && p.y <= y2;
		return containsX && containsY;
	}

	public boolean intersects(BoundingBox box2) {
		return (x1 <= box2.x2 && x2 >= box2.x1 && y1 <= box2.y2 && y2 >= box2.y1);
	}
}
