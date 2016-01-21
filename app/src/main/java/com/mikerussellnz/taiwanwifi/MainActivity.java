package com.mikerussellnz.taiwanwifi;

import android.graphics.Color;
import android.hardware.SensorManager;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikerussellnz.taiwanwifi.Clustering.Cluster;
import com.mikerussellnz.taiwanwifi.Clustering.Clusterer;
import com.mikerussellnz.taiwanwifi.Clustering.ImprovedClusterer;
import com.mikerussellnz.taiwanwifi.DataImport.DataImportCompletedListener;
import com.mikerussellnz.taiwanwifi.DataImport.iTaiwanImporter;
import com.mikerussellnz.taiwanwifi.Location.BearingSource;
import com.mikerussellnz.taiwanwifi.Location.LocationChangedFlags;
import com.mikerussellnz.taiwanwifi.Location.LocationChangedListener;
import com.mikerussellnz.taiwanwifi.Location.LocationManager;
import com.mikerussellnz.taiwanwifi.MapData.MapDataAvailableListener;
import com.mikerussellnz.taiwanwifi.MapData.MapDataFileRetriever;
import com.mikerussellnz.taiwanwifi.Mapping.BoundingBoxUtils;
import com.mikerussellnz.taiwanwifi.Mapping.LayerGroup;
import com.mikerussellnz.taiwanwifi.Mapping.MarkerTappedListener;

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
import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener,
		GoogleApiClient.ConnectionCallbacks,
		MarkerTappedListener,
		Observer, LocationChangedListener {

	private GoogleApiClient _googleApiClient;
	private MapView _mapView;
	private FloatingActionButton _actionButton;
	private UserLocation _userLocation;

	private LocationManager _locationManager;

	private View _popupView;
	private Marker _selectedMarker;

	private BoundingBox _mapBounds;
	private TextView _debugText;
	private boolean _trackingLocation = false;

	private LayerGroup _hotSpotsLayer;
	private LayerGroup _overlaysLayer;
	private ArrayList<HotSpot> _allHotSpots;

	private Clusterer<HotSpot> _clusterer;

	protected synchronized void buildGoogleApiClient() {
		_googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
		_googleApiClient.connect();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidGraphicFactory.createInstance(this.getApplication());
		setContentView(R.layout.activity_main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		_mapView = (MapView) findViewById(R.id.mapView);
		_actionButton = (FloatingActionButton) findViewById(R.id.fab);
		_debugText = (TextView) findViewById(R.id.debugText);

		/* FloatingActionButton settingsFab = (FloatingActionButton) findViewById(R.id.settingsFab);
		settingsFab.setImageDrawable(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_settings).color(Color.DKGRAY));

		settingsFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
				startActivity(intent);
			}
		}); */

		_clusterer = new ImprovedClusterer<HotSpot>(this, _mapView) {
			@Override
			public Layer getDisplayLayerSingleItem(HotSpot item) {
				return item;
			}

			@Override
			public Layer getDisplayLayerCluster(Cluster cluster) {
				return getMapMarker(cluster);
			}

			@Override
			public void addLayersToMap(Collection<Layer> layers) {
				_hotSpotsLayer.addAll(layers);
			}

			@Override
			public void removeLayersFromMap(Collection<Layer> layers) {
				_hotSpotsLayer.removeAll(layers);
			}

			@Override
			protected void onRecluster(ArrayList<Cluster<HotSpot>> newClusters) {
				super.onRecluster(newClusters);
				boolean stillCanSeeSelectedMarker = false;

				for (Cluster<HotSpot> cluster : newClusters) {
					if (cluster.getItems().size() == 1 && cluster.getItems().get(0) == _selectedMarker) {
						stillCanSeeSelectedMarker = true;
					}
				}

				if (!stillCanSeeSelectedMarker) {
					deselectSelectedMarker();
				}
			}
		};


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
		_mapView.setBuiltInZoomControls(true);

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

		_userLocation = new UserLocation(this, _mapView, null, 0);
		_overlaysLayer.add(_userLocation);

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
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		_locationManager = new LocationManager(_googleApiClient, sensorManager);
		_locationManager.setLocationChangedListener(this);

		Realm realm = Realm.getInstance(this);
		final HotSpotList list = new HotSpotList(realm);

		if (list.count() == 0) {
			iTaiwanImporter.importList(this, "raw/hotspotlist_en", new DataImportCompletedListener() {
				@Override
				public void onDataImportComplete() {
					loadAllHotSpots(list);
				}
			});
		} else {
			loadAllHotSpots(list);
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

		_clusterer.addAll(_allHotSpots);
		System.out.println("done adding to clusterer.");
	}

	private Marker getMapMarker(Cluster<HotSpot> cluster) {
		if (cluster.getItems().size() == 1) {
			HotSpot hotSpot = cluster.getItems().get(0);
			return hotSpot;
		} else {
			ClusterMarker marker = new ClusterMarker(cluster,
					ClusterMarker.getIcon(this, cluster.getItems().size()));
			marker.setMarkerTappedListener(this);
			return marker;
		}
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
		LatLong currentLocation = _locationManager != null ? _locationManager.getCurrentLocation() : null;
		if (currentLocation == null) {
			return false;
		}
		if (_mapBounds != null && !_mapBounds.contains(currentLocation)) {
			if (!isTrackingUpdate) {
				Snackbar.make(_mapView, "You are not in Taiwan!", Snackbar.LENGTH_LONG).show();
				return false;
			}
		} else {
			if (!isTrackingUpdate) {
				BoundingBox box = BoundingBoxUtils.boxForLatLon(currentLocation);
				box = box.extendMeters(200);
				zoomToBoundingBox(box, false, true);
			} else {
				_mapView.setCenter(currentLocation);
			}
		}
		return true;
	}

	@Override
	public void onConnected(Bundle bundle) {
		System.out.println("services connected");
		LatLong latLon = _locationManager.startLocationUpdates();

		if (latLon == null) {
			Snackbar waiting = Snackbar.make(_mapView, "Waiting for location...", Snackbar.LENGTH_LONG);
			waiting.show();
		}

		if (latLon != null) {
			_userLocation.setLatLong(latLon);
			_overlaysLayer.requestRedraw();
		}

		panToCurrentLocation(false);
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
	protected void onPause() {
		super.onPause();
		_locationManager.stopLocationUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		_locationManager.startLocationUpdates();
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

		// now pan the map so the entire popup is on screen.
		_popupView.postDelayed(_popViewPanMap, 10);
		return false;
	}

	private Runnable _popViewPanMap = new Runnable() {
		@Override
		public void run() {
			if (_popupView.getWidth() > 0 && _popupView.getHeight() > 0) {
				float x = _popupView.getX();
				float y = _popupView.getY();
				int w = _popupView.getWidth();
				int h = _popupView.getHeight();

				int mapWidth = _mapView.getWidth();
				int mapHeight = _mapView.getHeight();

				int bufferSize = Utils.getDip(getBaseContext(), 10);

				int moveX = 0;
				int moveY = 0;

				if (x < 0) {
					moveX = Math.round(x * -1) + bufferSize;
				} else if ((x + w) > mapWidth) {
					moveX = (Math.round(((x + w) - mapWidth)) + bufferSize) * -1;
				}

				if (y < 0) {
					moveY = Math.round(y * -1) + bufferSize;
				} else if ((y + h) > mapHeight) {
					moveY = (Math.round(((y + h) - mapHeight)) + bufferSize) * -1;
				}

				_mapView.getModel().mapViewPosition.moveCenter(moveX, moveY, true);
			} else {
				System.out.println("Popup view does not have w/h, re-posting until it does renders.");
				_popupView.postDelayed(_popViewPanMap, 10);
			}
		}
	};

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

	@Override
	public void onLocatonChanged(LatLong latLon, int accuracy, int bearing, BearingSource bearingSource, LocationChangedFlags changeFlags) {
		_debugText.setText(_locationManager.getDebugText());

		if (changeFlags.hasFlag(LocationChangedFlags.LOCATION_CHANGED)) {
			_userLocation.setLatLong(latLon);
		}

		if (changeFlags.hasFlag(LocationChangedFlags.ACCURACY_CHANGED)) {
			_userLocation.setZoneRadius(accuracy);
		}

		if (changeFlags.hasFlag(LocationChangedFlags.BEARING_CHANGED)) {
			_userLocation.setBearing(bearing);
		}

		if (changeFlags.hasFlag(LocationChangedFlags.BEARING_SOURCE_CHANGED)) {
			if (bearingSource == BearingSource.LOCATION_PROVIDER) {
				_userLocation.setDebugHasGpsBearing(true);
			} else {
				_userLocation.setDebugHasGpsBearing(false);
			}
		}

		_overlaysLayer.requestRedraw();

		if (_trackingLocation) {
			panToCurrentLocation(true);
		}
	}
}

