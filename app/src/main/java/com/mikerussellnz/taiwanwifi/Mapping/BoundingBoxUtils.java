package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;

/**
 * Created by mike on 1/12/15.
 */
public class BoundingBoxUtils {

	public static BoundingBox boxForLatLon(LatLong latLon) {
		// box needs a non-zero size for extend to work properly.
		BoundingBox box = new BoundingBox(latLon.latitude - 0.000005,
				latLon.longitude - 0.000005,
				latLon.latitude + 0.000005,
				latLon.longitude + 0.000005);
		return box;
	}
}
