package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.overlay.Circle;

/**
 * Created by mike on 6/01/16.
 */
public class CircleWithText extends Circle {
	private String _text;
	private Paint _paintText;
	private int _offsetX = 0;
	private int _offsetY = 0;

	public CircleWithText(LatLong latLong, float radius, String text, Paint paintText, Paint paintFill, Paint paintStroke) {
		super(latLong, radius, paintFill, paintStroke);
		_paintText = paintText;
		_text = text;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		if (getPosition() == null || (getPaintStroke() == null && getPaintFill() == null)) {
			return;
		}

		double latitude = getPosition().latitude;
		double longitude = getPosition().longitude;
		int tileSize = displayModel.getTileSize();
		long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
		int pixelX = (int) (MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x);
		int pixelY = (int) (MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y);
		int radiusInPixel = getRadiusInPixels(latitude, zoomLevel);

		Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		if (!canvasRectangle.intersectsCircle(pixelX, pixelY, radiusInPixel)) {
			return;
		}

		if (getPaintStroke() != null) {
			canvas.drawCircle(pixelX, pixelY, radiusInPixel, getPaintStroke());
		}
		if (getPaintFill() != null) {
			canvas.drawCircle(pixelX, pixelY, radiusInPixel, getPaintFill());
		}
		if (getPaintText() != null) {
			canvas.drawText(_text, pixelX + _offsetX, pixelY + _offsetY, getPaintText());
		}
	}

	private Paint getPaintText() {
		return _paintText;
	}

	public void setOffsetX(int offsetX) {
		_offsetX = offsetX;
	}

	public void setOffsetY(int offsetY) {
		_offsetY = offsetY;
	}
}