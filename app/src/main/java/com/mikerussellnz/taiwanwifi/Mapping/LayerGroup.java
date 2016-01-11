package com.mikerussellnz.taiwanwifi.Mapping;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.util.MapViewProjection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;

// no idea why mapsforge doesn't have a way of grouping items out of the box.
public class LayerGroup extends Layer implements Iterable<Layer>, RandomAccess {
	private final List<Layer> _layerList;
	private MapViewProjection _projection;

	private static void checkIsNull(Collection<Layer> layers) {
		if (layers == null) {
			throw new IllegalArgumentException("layers must not be null");
		}

		for (Layer layer : layers) {
			checkIsNull(layer);
		}
	}

	private static void checkIsNull(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException("layer must not be null");
		}
	}

	public LayerGroup(MapView mapView) {
		_projection = new MapViewProjection(mapView);
		_layerList = new CopyOnWriteArrayList<>();
	}

	public synchronized void add(int index, Layer layer) {
		checkIsNull(layer);
		layer.setDisplayModel(displayModel);
		_layerList.add(index, layer);
		requestRedraw();
	}

	public synchronized void add(Layer layer) {
		checkIsNull(layer);
		layer.setDisplayModel(displayModel);

		_layerList.add(layer);
		requestRedraw();
	}

	public synchronized void addAll(Collection<Layer> layers) {
		checkIsNull(layers);
		for (Layer layer : layers) {
			layer.setDisplayModel(displayModel);
		}
		_layerList.addAll(layers);
		requestRedraw();
	}

	public synchronized void addAll(int index, Collection<Layer> layers) {
		checkIsNull(layers);
		_layerList.addAll(index, layers);
		for (Layer layer : layers) {
			layer.setDisplayModel(displayModel);
		}
		requestRedraw();
	}

	public synchronized void clear() {
		_layerList.clear();
		requestRedraw();
	}

	public synchronized boolean contains(Layer layer) {
		checkIsNull(layer);
		return _layerList.contains(layer);
	}

	public synchronized Layer get(int index) {
		return _layerList.get(index);
	}

	public synchronized int indexOf(Layer layer) {
		checkIsNull(layer);
		return _layerList.indexOf(layer);
	}

	public synchronized boolean isEmpty() {
		return _layerList.isEmpty();
	}

	@Override
	public synchronized Iterator<Layer> iterator() {
		return _layerList.iterator();
	}

	public synchronized Layer remove(int index) {
		Layer layer = _layerList.remove(index);
		requestRedraw();
		return layer;
	}

	public synchronized boolean remove(Layer layer) {
		checkIsNull(layer);
		if (_layerList.remove(layer)) {
			requestRedraw();
			return true;
		}
		return false;
	}

	@Override
	public synchronized void setDisplayModel(DisplayModel displayModel) {
		super.setDisplayModel(displayModel);
		for (Layer layer : _layerList) {
			layer.setDisplayModel(displayModel);
		}
	}

	public synchronized int size() {
		return _layerList.size();
	}

	@Override
	public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
		for (Layer layer : _layerList) {
			if (layer.isVisible()) {
				layer.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
			}
		}
	}

	@Override
	public synchronized boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
		for (Layer layer : _layerList) {
			Point innerLayerXY = _projection.toPixels(layer.getPosition());
			if (layer.onTap(tapLatLong, innerLayerXY, tapXY)) {
				return true;
			}
		}
		return false;
	}

}
