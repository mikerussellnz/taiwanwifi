package com.mikerussellnz.taiwanwifi.Location;

import org.mapsforge.core.model.LatLong;

/**
 * Created by mike on 17/01/16.
 */
public interface LocationChangedListener {
	void onLocatonChanged(LatLong latLon, int accuracy, int bearing, BearingSource bearingSource, LocationChangedFlags changeFlags);
}
