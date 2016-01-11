package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * Created by mike on 29/12/15.
 */
public class MarkerWithRadius<T extends Marker> extends LayerGroup {
	protected Circle _circle;
	protected T _marker;

	public MarkerWithRadius(MapView mapView, T marker, int zoneRadius) {
		super(mapView);
		_marker = marker;
		_circle = new Circle(_marker.getLatLong(), zoneRadius, null, null);
		add(_circle);
		add(_marker);
	}

	public void setZoneRadius(float zoneRadius) {
		_circle.setRadius(zoneRadius);
	}

	public void setZonePaintFill(Paint zonePaintFill) {
		_circle.setPaintFill(zonePaintFill);
	}

	public void setZonePaintStroke(Paint zonePaintStroke) {
		_circle.setPaintStroke(zonePaintStroke);
	}

	public synchronized void setLatLong(LatLong latLong) {
		_marker.setLatLong(latLong);
		_circle.setLatLong(latLong);
	}

	public synchronized LatLong getLatLong() {
		return _marker.getLatLong();
	}
}
