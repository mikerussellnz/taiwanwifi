package com.mikerussellnz.taiwanwifi.Location;

/**
 * Created by mike on 17/01/16.
 */
public class LocationChangedFlags {
	public static final int LOCATION_CHANGED = 1;
	public static final int ACCURACY_CHANGED = 2;
	public static final int BEARING_CHANGED = 4;
	public static final int BEARING_SOURCE_CHANGED = 8;

	private int _flagsValue = 0;

	public LocationChangedFlags(int flags) {
		_flagsValue = flags;
	}

	public boolean hasFlag(int flag) {
		return (_flagsValue & flag) == flag;
	}
}
