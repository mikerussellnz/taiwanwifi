package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

/**
 * Created by mike on 30/11/15.
 */
public class RotatingMarker extends TapableMarker {
	private int _rotation = 0;

	public RotatingMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
		super(latLong, bitmap, horizontalOffset, verticalOffset);
	}

	public int getRotation() {
		return _rotation;
	}

	public void setRotation(int rotation) {
		_rotation = rotation;
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		android.graphics.Canvas androidCanvas = AndroidGraphicFactory.getCanvas(canvas);

		LatLong latLong = getLatLong();
		Bitmap bitmap = getBitmap();
		int horizontalOffset = getHorizontalOffset();
		int verticalOffset = getVerticalOffset();

		if (latLong == null || bitmap == null) {
			return;
		}

		long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
		double pixelX = MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize);
		double pixelY = MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize);

		int halfBitmapWidth = bitmap.getWidth() / 2;
		int halfBitmapHeight = bitmap.getHeight() / 2;

		int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + horizontalOffset);
		int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + verticalOffset);
		int right = left + bitmap.getWidth();
		int bottom = top + bitmap.getHeight();

		Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);

		Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
		if (!canvasRectangle.intersects(bitmapRectangle)) {
			return;
		}

		androidCanvas.save();

		Point center = bitmapRectangle.getCenter();
		androidCanvas.rotate(_rotation, (float) center.x, (float) center.y);

		canvas.drawBitmap(bitmap, left, top);

		androidCanvas.restore();
	}
}
