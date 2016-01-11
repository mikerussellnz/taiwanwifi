package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * Created by mike on 30/11/15.
 */
public interface MarkerTappedListener {
	boolean onTapped(Marker marker, LatLong tapLatLong, Point layerXY, Point tapXY);
}
