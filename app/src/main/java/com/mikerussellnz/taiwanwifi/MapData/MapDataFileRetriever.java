package com.mikerussellnz.taiwanwifi.MapData;

import android.app.Activity;
import android.content.Context;

import com.mikerussellnz.taiwanwifi.IOUtils;
import com.mikerussellnz.taiwanwifi.ModalTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mike on 29/12/15.
 */
public class MapDataFileRetriever {

	public static void retrieveMapFile(Activity ctx, String mapFile, MapDataAvailableListener callback) {

		File file = new File(ctx.getFilesDir(), mapFile);
		// map file already extracted.
		if (file.exists()) {
			callback.onMapDataAvailable(file);
			return;
		}

		UnpackMapTask unpackMapTask = new UnpackMapTask(ctx, callback);
		unpackMapTask.execute(mapFile);
	}

	public static class UnpackMapTask extends ModalTask<String, Integer, File> {
		private MapDataAvailableListener _callback;

		public UnpackMapTask(Activity ctx, MapDataAvailableListener callback) {
			super(ctx);
			_callback = callback;
		}

		@Override
		protected CharSequence getTitle() {
			return "Please Wait...";
		}

		@Override
		protected CharSequence getMessage() {
			return "Preparing Map Data";
		}

		@Override
		protected void onPostExecute(File file) {
			super.onPostExecute(file);

			if (file == null) {
				// TODO: display error dialog.
				return;
			}

			_callback.onMapDataAvailable(file);
		}

		@Override
		protected File doInBackground(String... params) {
			super.doInBackground(params);

			String mapFile = params[0];

			Context ctx = getContext();

			File file = new File(ctx.getFilesDir(), mapFile);
			InputStream input = null;
			OutputStream output = null;
			try {
				OutputStream outputStream = new FileOutputStream(file);
				InputStream inputStream = ctx.getAssets().open(mapFile);
				IOUtils.copyStream(inputStream, outputStream);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
					}
				}
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
					}
				}
			}

			return file;
		}
	}
}
