package com.mikerussellnz.taiwanwifi.Location;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.mapsforge.core.model.LatLong;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mike on 17/01/16.
 */
public class LocationManager implements LocationListener, SensorEventListener {
	private LocationChangedListener _locationChangedListener;
	private GoogleApiClient _googleApiClient;
	private SensorManager _sensorManager;

	private LatLong _currentLocation;
	private int _currentAccuracy;
	private int _currentBearing;
	private BearingSource _currentBearingSource;
	private String _debugText;
	private boolean _hasLocationProviderBearing;

	protected LocationRequest createLocationRequest() {
		LocationRequest mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(1000);
		mLocationRequest.setFastestInterval(1000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		return mLocationRequest;
	}

	public LocationManager(GoogleApiClient googleApiClient, SensorManager sensorManager) {
		_googleApiClient = googleApiClient;
		_sensorManager = sensorManager;
	}

	public LocationChangedListener getLocationChangedListener() {
		return _locationChangedListener;
	}

	public void setLocationChangedListener(LocationChangedListener locationChangedListener) {
		_locationChangedListener = locationChangedListener;
	}

	public LatLong getCurrentLocation() {
		return _currentLocation;
	}

	public int getCurrentBearing() {
		return _currentBearing;
	}

	public int getCurrentAccuracy() {
		return _currentAccuracy;
	}

	public String getDebugText() { return _debugText; }

	public LatLong startLocationUpdates() {
		LatLong latLon = null;

		if (_googleApiClient.isConnected()) {
			Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
					_googleApiClient);

			if (lastLocation != null) {
				latLon = new LatLong(lastLocation.getLatitude(), lastLocation.getLongitude());
			}
			_currentLocation = latLon;

			LocationServices.FusedLocationApi.requestLocationUpdates(
					_googleApiClient, createLocationRequest(), this);
		}
		_sensorManager.registerListener(this, _sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);

		return latLon;
	}

	public void stopLocationUpdates() {
		if (_googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient, this);
		}
		_sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		int bearing = Math.round(event.values[0]);

		if (_hasLocationProviderBearing) {
			return;
		}

		int changeFlags = 0;

		if (_currentBearingSource != BearingSource.MAGNETOMETER) {
			changeFlags |= LocationChangedFlags.BEARING_SOURCE_CHANGED;
			_currentBearingSource = BearingSource.MAGNETOMETER;
		}

		if (_currentBearing != bearing) {
			changeFlags |= LocationChangedFlags.BEARING_CHANGED;
			_currentBearing = bearing;
		}

		if (changeFlags != 0 && _locationChangedListener != null) {
			_locationChangedListener.onLocatonChanged(
					_currentLocation, _currentAccuracy,
					_currentBearing, _currentBearingSource,
					new LocationChangedFlags(changeFlags));
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onLocationChanged(Location location) {
		LatLong latLon = new LatLong(location.getLatitude(), location.getLongitude());

		int bearing = Math.round(location.getBearing());
		int accuracy = Math.round(location.getAccuracy());

		System.out.println("Using provider: " + location.getProvider());
		System.out.println("Location provider bearing: " + location.hasBearing() + ": " + bearing);

		SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		_debugText = "Provider: " + location.getProvider() + "\n"
				+ "Time: " + sd.format(new Date(location.getTime())) + "\n"
				+ "Location: " + location.getLatitude() + "," + location.getLongitude() + "\n"
				+ "Accuracy: " + location.hasAccuracy() + ": " + accuracy + "\n"
				+ "Bearing: " + location.hasBearing() + ": " + bearing + "\n"
				+ "Speed: " + location.hasSpeed() + ": " + location.getSpeed() + "\n"
				+ "Altitude: " + location.hasAltitude() + ": " + location.getAltitude();

		int changeFlags = 0;

		if (accuracy != _currentAccuracy) {
			_currentAccuracy = accuracy;
			changeFlags |= LocationChangedFlags.ACCURACY_CHANGED;
		}

		boolean hasBearing = location.hasBearing();

		if (_hasLocationProviderBearing != hasBearing) {
			_hasLocationProviderBearing = hasBearing;
		}

		if (hasBearing && _currentBearingSource != BearingSource.LOCATION_PROVIDER) {
			_currentBearingSource = BearingSource.LOCATION_PROVIDER;
			changeFlags |= LocationChangedFlags.BEARING_SOURCE_CHANGED;
		}

		if (_currentBearing != bearing) {
			_currentBearing = bearing;
			changeFlags |= LocationChangedFlags.BEARING_CHANGED;
		}

		if (!latLon.equals(_currentLocation)) {
			_currentLocation = latLon;
			changeFlags |= LocationChangedFlags.LOCATION_CHANGED;
		}

		if (changeFlags != 0 && _locationChangedListener != null) {
			_locationChangedListener.onLocatonChanged(
					_currentLocation, _currentAccuracy,
					_currentBearing, _currentBearingSource,
					new LocationChangedFlags(changeFlags));
		}
	}
}
