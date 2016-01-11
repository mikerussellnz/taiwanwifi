package com.mikerussellnz.taiwanwifi;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikerussellnz.taiwanwifi.Clustering.Cluster;
import com.mikerussellnz.taiwanwifi.Clustering.Clusterer;
import com.mikerussellnz.taiwanwifi.DataImport.DataImportCompletedListener;
import com.mikerussellnz.taiwanwifi.DataImport.iTaiwanImporter;
import com.mikerussellnz.taiwanwifi.MapData.MapDataAvailableListener;
import com.mikerussellnz.taiwanwifi.MapData.MapDataFileRetriever;
import com.mikerussellnz.taiwanwifi.Mapping.BoundingBoxUtils;
import com.mikerussellnz.taiwanwifi.Mapping.LayerGroup;
import com.mikerussellnz.taiwanwifi.Mapping.MarkerTappedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mikepenz.iconics.IconicsDrawable;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener,
		GoogleApiClient.ConnectionCallbacks,
		LocationListener,
		SensorEventListener,
		MarkerTappedListener,
		Observer {

	private GoogleApiClient _googleApiClient;
	private SensorManager _sensorManager;
	private MapView _mapView;
	private FloatingActionButton _actionButton;
	private UserLocation _userLocation;

	private View _popupView;
	private Marker _selectedMarker;

	private double _oldZoomLevel = -1.0;

	private LatLong _currentLocation;
	private int _currentAccuracy;
	private int _currentBearing;

	private BoundingBox _mapBounds;
	private TextView _debugText;
	private boolean _trackingLocation = false;

	private boolean _hasLocationBearing;

	private LayerGroup _hotSpotsLayer;
	private LayerGroup _overlaysLayer;
	private ArrayList<HotSpot> _allHotSpots;

	protected synchronized void buildGoogleApiClient() {
		_googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
		_googleApiClient.connect();
	}

	protected LocationRequest createLocationRequest() {
		LocationRequest mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(1000);
		mLocationRequest.setFastestInterval(1000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		return mLocationRequest;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidGraphicFactory.createInstance(this.getApplication());
		setContentView(R.layout.activity_main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		_mapView = (MapView) findViewById(R.id.mapView);
		_actionButton = (FloatingActionButton) findViewById(R.id.fab);

		/* FloatingActionButton settingsFab = (FloatingActionButton) findViewById(R.id.settingsFab);
		settingsFab.setImageDrawable(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_settings).color(Color.DKGRAY));

		settingsFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
				startActivity(intent);
			}
		}); */

		_debugText = (TextView) findViewById(R.id.debugText);

		updateTrackingStatus(false);
		_actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateTrackingStatus(!_trackingLocation);
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			_mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}

		_hotSpotsLayer = new LayerGroup(_mapView);
		_mapView.addLayer(_hotSpotsLayer);
		_overlaysLayer = new LayerGroup(_mapView);
		_mapView.addLayer(_overlaysLayer);

		_mapView.setClickable(true);

		_mapView.getMapScaleBar().setVisible(true);
		_mapView.setBuiltInZoomControls(false);

		_mapView.setGestureDetector(new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				updateTrackingStatus(false);
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				updateTrackingStatus(false);
				return super.onFling(e1, e2, velocityX, velocityY);
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				deselectSelectedMarker();
				return super.onSingleTapConfirmed(e);
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (_trackingLocation) {
					MapViewPosition mapViewPosition = _mapView.getModel().mapViewPosition;
					mapViewPosition.setZoomLevel((byte) (mapViewPosition.getZoomLevel() + 1));
					return true;
				}
				return super.onDoubleTap(e);
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return super.onDoubleTapEvent(e);
			}
		}));

		final Model model = _mapView.getModel();
		MapViewPosition position = model.mapViewPosition;
		position.setZoomLevelMin((byte) 7);
		position.setZoomLevelMax((byte) 20);
		position.addObserver(this);

		MapDataFileRetriever.retrieveMapFile(this, "taiwan.map", new MapDataAvailableListener() {
			@Override
			public void onMapDataAvailable(File file) {
				setMapDataSource(_mapView, file);
			}
		});

		buildGoogleApiClient();
		_sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		Realm realm = Realm.getInstance(this);
		final HotSpotList list = new HotSpotList(realm);

		if (list.count() == 0) {
			iTaiwanImporter.importList(this, "raw/hotspotlist_en", new DataImportCompletedListener() {
				@Override
				public void onDataImportComplete() {
					loadAllHotSpots(list);
					addHotSpotsToMap();
				}
			});
		} else {
			loadAllHotSpots(list);
			addHotSpotsToMap();
		}
	}

	private void loadAllHotSpots(HotSpotList list) {
		System.out.println("querying...");
		_allHotSpots = new ArrayList<>(list.count());
		for (HotSpotModel fields : list.all()) {
			HotSpot hotSpot = new HotSpot(fields, HotSpot.getIcon(this));
			hotSpot.setMarkerTappedListener(this);
			_allHotSpots.add(hotSpot);
		}
		System.out.println("done querying");
	}

	private void addHotSpotsToMap() {
		if (_allHotSpots == null) {
			return;
		}

		Clusterer<HotSpot> clusterer = new Clusterer<>(_mapView);
		clusterer.addAll(_allHotSpots);
		List<Cluster<HotSpot>> clusters = clusterer.getClusters();

		boolean stillCanSeeSelectedMarker = false;

		ArrayList<Layer> mapDisplayItems = new ArrayList<>(clusters.size());

		for (Cluster<HotSpot> cluster : clusters) {
			if (cluster.getItems().size() == 1) {
				HotSpot hotSpot = cluster.getItems().get(0);
				if (_selectedMarker == hotSpot) {
					stillCanSeeSelectedMarker = true;
				}
				mapDisplayItems.add(hotSpot);
			} else {
				ClusterMarker marker = new ClusterMarker(cluster,
						ClusterMarker.getIcon(this, cluster.getItems().size()));
				marker.setMarkerTappedListener(this);
				mapDisplayItems.add(marker);
			}
		}

		if (!stillCanSeeSelectedMarker) {
			deselectSelectedMarker();
		}

		_hotSpotsLayer.addAll(mapDisplayItems);
	}

	private void deselectSelectedMarker() {
		_selectedMarker = null;
		removePopup();
	}

	private void setMapDataSource(MapView mapView, File mapDataFile) {
		final Model model = mapView.getModel();

		TileCache tileCache = AndroidUtil.createTileCache(
				this, "mapcache", model.displayModel.getTileSize(), 1f, model.frameBufferModel.getOverdrawFactor());

		MapDataStore mapDataStore = new MapFile(mapDataFile);
		_mapBounds = mapDataStore.boundingBox();

		TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
				model.mapViewPosition, false, true, AndroidGraphicFactory.INSTANCE);
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

		MapViewPosition position = model.mapViewPosition;
		position.setMapLimit(_mapBounds);

		if (!panToCurrentLocation(false)) {
			System.out.println("zooming to map bounds");
			zoomToBoundingBox(_mapBounds, false, false);
		}

		System.out.println("Adding tiles");
		Layers layers = _mapView.getLayerManager().getLayers();
		layers.add(0, tileRendererLayer);
	}

	private void updateTrackingStatus(boolean trackUser) {
		_trackingLocation = trackUser;
		if (_trackingLocation) {
			final int color = getResources().getColor(R.color.colorPrimary);
			_actionButton.setImageDrawable(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_gps_dot).color(color));
			panToCurrentLocation(false);
		} else {
			_actionButton.setImageDrawable(new IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_gps_dot).color(Color.DKGRAY));
		}
	}

	private boolean panToCurrentLocation(boolean isTrackingUpdate) {
		if (_currentLocation == null) {
			return false;
		}
		if (_mapBounds != null && !_mapBounds.contains(_currentLocation)) {
			if (!isTrackingUpdate) {
				Snackbar.make(_mapView, "You are not in Taiwan!", Snackbar.LENGTH_LONG).show();
				return false;
			}
		} else {
			if (!isTrackingUpdate) {
				BoundingBox box = BoundingBoxUtils.boxForLatLon(_currentLocation);
				box = box.extendMeters(200);
				zoomToBoundingBox(box, false, true);
			} else {
				_mapView.setCenter(_currentLocation);
			}
		}
		return true;
	}

	@Override
	public void onConnected(Bundle bundle) {
		System.out.println("services connected");
		Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
				_googleApiClient);

		LatLong latLon = null;
		if (lastLocation != null) {
			latLon = new LatLong(lastLocation.getLatitude(), lastLocation.getLongitude());
		} else {
			Snackbar waiting = Snackbar.make(_mapView, "Waiting for location...", Snackbar.LENGTH_LONG);
			waiting.show();
		}

		_currentLocation = latLon;

		_userLocation = new UserLocation(this, _mapView, _currentLocation, 0);

		_overlaysLayer.add(_userLocation);

		panToCurrentLocation(false);
		startLocationUpdates();
	}

	private void zoomToBoundingBox(BoundingBox box, boolean allowZoomOut, boolean animate) {
		Model model = _mapView.getModel();

		byte zoom = LatLongUtils.zoomForBounds(
				_mapView.getDimension(),
				box,
				model.displayModel.getTileSize());

		LatLong location = box.getCenterPoint();
		if (animate) {
			model.mapViewPosition.animateTo(location);
		} else {
			model.mapViewPosition.setCenter(location);
		}

		if (zoom >= model.mapViewPosition.getZoomLevel() || allowZoomOut) {
			model.mapViewPosition.setZoomLevel(zoom, animate);
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		System.out.println("services suspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		System.out.println("services failed");
	}

	@Override
	public void onLocationChanged(Location location) {
		LatLong latLon = new LatLong(location.getLatitude(), location.getLongitude());

		int bearing = Math.round(location.getBearing());
		int accuracy = Math.round(location.getAccuracy());

		System.out.println("Using provider: " + location.getProvider());
		System.out.println("Location provider bearing: " + location.hasBearing() + ": " + bearing);

		SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		_debugText.setText("Provider: " + location.getProvider() + "\n"
				+ "Time: " + sd.format(new Date(location.getTime())) + "\n"
				+ "Location: " + location.getLatitude() + "," + location.getLongitude() + "\n"
				+ "Accuracy: " + location.hasAccuracy() + ": " + accuracy + "\n"
				+ "Bearing: " + location.hasBearing() + ": " + bearing + "\n"
				+ "Speed: " + location.hasSpeed() + ": " + location.getSpeed() + "\n"
				+ "Altitude: " + location.hasAltitude() + ": " + location.getAltitude());

		_userLocation.setLatLong(latLon);

		if (accuracy != _currentAccuracy) {
			_userLocation.setZoneRadius(accuracy);
			_overlaysLayer.requestRedraw();
		}

		_hasLocationBearing = location.hasBearing();

		if (_hasLocationBearing) {
			_userLocation.setDebugHasGpsBearing(true);
			if (_currentBearing != bearing) {
				_userLocation.setBearing(bearing);
				_overlaysLayer.requestRedraw();
				_currentBearing = bearing;
			}
		}

		_currentLocation = latLon;
		_currentAccuracy = accuracy;

		if (_trackingLocation) {
			panToCurrentLocation(true);
		}
	}

	public void startLocationUpdates() {
		if (_googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.requestLocationUpdates(
					_googleApiClient, createLocationRequest(), this);
		}
	}

	public void stopLocationUpdates() {
		if (_googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient, this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLocationUpdates();
		_sensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startLocationUpdates();
		_sensorManager.registerListener(this, _sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		int bearing = Math.round(event.values[0]);
		if (_userLocation != null && !_hasLocationBearing) {
			_userLocation.setDebugHasGpsBearing(false);
			if (_currentBearing != bearing) {
				_userLocation.setBearing(bearing);
				_overlaysLayer.requestRedraw();
				_currentBearing = bearing;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private void addOrMovePopup(Marker marker) {
		removePopup();
		_mapView.addView(_popupView, new MapView.LayoutParams(
				MapView.LayoutParams.WRAP_CONTENT,
				MapView.LayoutParams.WRAP_CONTENT,
				marker.getLatLong(),
				MapView.LayoutParams.Alignment.BOTTOM_CENTER));
	}

	private void removePopup() {
		_mapView.removeView(_popupView);
	}

	@Override
	public boolean onTapped(Marker marker, LatLong tapLatLong, Point layerXY, Point tapXY) {
		if (marker.getClass() == ClusterMarker.class) {
			ClusterMarker clusterMarker = (ClusterMarker)marker;
			Cluster<HotSpot> cluster = clusterMarker.getCluster();
			System.out.println("-----------");
			for (HotSpot hs : cluster.getItems()) {
				System.out.println(hs.getName());
			}
			return false;
		}

		if (_popupView == null) {
			_popupView = getLayoutInflater().inflate(R.layout.hotspot_popup, _mapView, false);
		}

		_selectedMarker = marker;

		TextView title = (TextView) _popupView.findViewById(R.id.title);
		TextView address = (TextView) _popupView.findViewById(R.id.address);

		System.out.println("Adding view for " + marker + " at location " + marker.getLatLong());
		HotSpot hs = (HotSpot)marker;
		title.setText(hs.getName());
		address.setText(hs.getAddress());
		addOrMovePopup(marker);

		return false;
	}

	private boolean mapCurrentlyBeingScaled() {
		Model model = _mapView.getModel();
		return model.mapViewPosition.animationInProgress();
	}

	private Runnable _readdPopupView = new Runnable() {
		@Override
		public void run() {
			if (_selectedMarker != null && !mapCurrentlyBeingScaled()) {
				addOrMovePopup(_selectedMarker);
			}
		}
	};

	@Override
	public void onChange() {
		// for re-clustering on zoom change - we only care about the zoom level increment
		// posted by the main thread at the end of the zoom animation.
		if (Thread.currentThread() == getMainLooper().getThread()) {
			int zoomLevel = _mapView.getModel().mapViewPosition.getZoomLevel();
			if (zoomLevel != _oldZoomLevel) {
				_hotSpotsLayer.clear();
				addHotSpotsToMap();
			}
			_oldZoomLevel = _mapView.getModel().mapViewPosition.getZoomLevel();
		}

		if (_popupView != null && mapCurrentlyBeingScaled()) {
			_popupView.post(new Runnable() {
				@Override
				public void run() {
					removePopup();
					_popupView.removeCallbacks(_readdPopupView);
					_popupView.postDelayed(_readdPopupView, 250);
				}
			});
		}
	}
}

