package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * Created by mike on 30/11/15.
 */
public class TapableMarker extends Marker {

	private MarkerTappedListener _markerTappedListener;

	public TapableMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
		super(latLong, bitmap, horizontalOffset, verticalOffset);
	}

	public void setMarkerTappedListener(MarkerTappedListener listener) {
		_markerTappedListener = listener;
	}

	@Override
	public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
		if (layerXY != null && contains(layerXY, tapXY)) {
			if (_markerTappedListener != null) {
				return _markerTappedListener.onTapped(this, tapLatLong, layerXY, tapXY);
			}
		}
		return false;
	}
}
