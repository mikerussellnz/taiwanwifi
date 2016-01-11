package com.mikerussellnz.taiwanwifi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;

import com.mikerussellnz.taiwanwifi.Clustering.Cluster;
import com.mikerussellnz.taiwanwifi.Mapping.TapableMarker;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.HashMap;

/**
 * Created by mike on 7/01/16.
 */
public class ClusterMarker extends TapableMarker {
	private Cluster<HotSpot> _cluster;

	private static HashMap<Integer, Bitmap> _cachedBitmaps = new HashMap<>();

	public static org.mapsforge.core.graphics.Bitmap getIcon(Context ctx, int count) {
		if (_cachedBitmaps.containsKey(count)) {
			return _cachedBitmaps.get(count);
		}
		org.mapsforge.core.graphics.Bitmap cc = drawClusterIcon(ctx, count);
		_cachedBitmaps.put(count, cc);
		return cc;
	}

	private static org.mapsforge.core.graphics.Bitmap drawClusterIcon(Context ctx, int count) {
		int px40 = Utils.getDip(ctx, 40);
		int px20 = Utils.getDip(ctx, 20);

		android.graphics.Bitmap tempBitmap = android.graphics.Bitmap.createBitmap(px40 + 4, px40 + 4, android.graphics.Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(tempBitmap);
		String text = Integer.toString(count);

		Paint paint = new Paint();
		paint.setColor(Color.rgb(114,132,232));
		paint.setAlpha(255);

		Paint strokePaint = new Paint();
		strokePaint.setColor(Color.BLACK);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeWidth(3);
		strokePaint.setAntiAlias(true);

		Paint textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setAntiAlias(true);
		if (text.length() > 3) {
			textPaint.setTextSize(Utils.getDip(ctx, 14));
		} else {
			textPaint.setTextSize(Utils.getDip(ctx, 16));
		}
		textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

		Rect textBounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), textBounds);

		canvas.drawCircle(px20 + 2, px20 + 2, px20, paint);
		canvas.drawCircle(px20 + 2, px20 + 2, px20, strokePaint);

		canvas.drawText(text, px20, px20 - textBounds.exactCenterY(), textPaint);

		return AndroidGraphicFactory.convertToBitmap(new BitmapDrawable(ctx.getResources(), tempBitmap));
	}

	public Cluster<HotSpot> getCluster() { return _cluster; }

	public ClusterMarker(Cluster<HotSpot> cluster, Bitmap bitmap) {
		super(cluster.getAnchorItem().getLatLong(), bitmap, 0, 0);
		_cluster = cluster;
	}
}

