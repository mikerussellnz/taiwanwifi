package com.mikerussellnz.taiwanwifi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Surface;
import android.view.WindowManager;

import com.mikerussellnz.taiwanwifi.Mapping.MarkerWithRadius;
import com.mikerussellnz.taiwanwifi.Mapping.RotatingMarker;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;

/**
 * Created by mike on 29/12/15.
 */
public class UserLocation extends MarkerWithRadius<RotatingMarker> {
	private Context _ctx;

	private static final int ICON_ROTATION_CORRECTION = -45;

	static Bitmap getIcon(Context ctx) {
		Drawable drawable = new IconicsDrawable(ctx).icon(FontAwesome.Icon.faw_location_arrow).sizeDp(30).color(Color.DKGRAY);
		return AndroidGraphicFactory.convertToBitmap(drawable);
	}

	public UserLocation(Context ctx, MapView mapView, LatLong currentLocation, int zoneRadius) {
		super(mapView, new RotatingMarker(currentLocation, getIcon(ctx), 0, 0), zoneRadius);
		_ctx = ctx;

		int blue = Color.rgb(61, 162, 239);

		org.mapsforge.core.graphics.Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
		Paint androidPaint = AndroidGraphicFactory.getPaint(paint);
		androidPaint.setColor(blue);
		androidPaint.setAlpha(120);

		org.mapsforge.core.graphics.Paint strokePaint = AndroidGraphicFactory.INSTANCE.createPaint();
		Paint androidStroke = AndroidGraphicFactory.getPaint(strokePaint);
		androidStroke.setColor(blue);
		androidStroke.setStyle(Paint.Style.STROKE);
		androidStroke.setStrokeWidth(2);

		setZonePaintFill(paint);
		setZonePaintStroke(strokePaint);
	}

	public void setDebugHasGpsBearing(boolean hasGps) {
		Drawable icon;
		if (hasGps) {
			icon = new IconicsDrawable(_ctx).icon(FontAwesome.Icon.faw_location_arrow).sizeDp(30).color(Color.RED);
		} else {
			icon = new IconicsDrawable(_ctx).icon(FontAwesome.Icon.faw_location_arrow).sizeDp(30).color(Color.DKGRAY);
		}
		_marker.setBitmap(AndroidGraphicFactory.convertToBitmap(icon));
	}

	public int getScreenRotationDegrees() {
		WindowManager windowManager = (WindowManager)_ctx.getSystemService(Context.WINDOW_SERVICE);
		int rotation = windowManager.getDefaultDisplay().getRotation();
		switch (rotation) {
			case Surface.ROTATION_0:
				return 0;
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
		}
		return 0;
	}

	public void setBearing(int bearing) {
		_marker.setRotation(bearing + getScreenRotationDegrees() + ICON_ROTATION_CORRECTION);
	}
}
