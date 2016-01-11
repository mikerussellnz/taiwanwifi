package com.mikerussellnz.taiwanwifi;

import android.content.Context;

/**
 * Created by mike on 8/01/16.
 */
public class Utils {
	public static int getDip(Context ctx, int pixel) {
		float scale = ctx.getResources().getDisplayMetrics().density;
		return (int) (pixel * scale + 0.5f);
	}
}
