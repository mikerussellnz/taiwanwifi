package com.mikerussellnz.taiwanwifi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.mikerussellnz.taiwanwifi.Mapping.TapableMarker;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

public class HotSpot extends TapableMarker {
	private static Bitmap _hotSpotBitmap;

	public static synchronized Bitmap getIcon(Context ctx) {
		if (_hotSpotBitmap == null) {
			Drawable drawable = new IconicsDrawable(ctx).icon(GoogleMaterial.Icon.gmd_place).color(Color.rgb(114,132,232)).contourWidthDp(2).sizeDp(40);
			_hotSpotBitmap = AndroidGraphicFactory.convertToBitmap(drawable);
		}
		return _hotSpotBitmap;
	}

	private HotSpotModel _fields;

	public String getName() {
		return _fields.getName();
	}

	public String getAddress() {
		return _fields.getAddress();
	}

	public HotSpot(HotSpotModel fields, Bitmap bitmap) {
		super(new LatLong(fields.getLat(), fields.getLon()), bitmap, 0, -bitmap.getHeight() / 2);
		_fields = fields;
	}
}
