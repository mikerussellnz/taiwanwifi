package com.mikerussellnz.taiwanwifi.DataImport;

import android.app.Activity;
import android.content.Context;

import com.mikerussellnz.taiwanwifi.HotSpotList;
import com.mikerussellnz.taiwanwifi.ModalTask;

import java.io.InputStream;

import io.realm.Realm;

/**
 * Created by mike on 29/12/15.
 */
public class iTaiwanImporter {

	public static void importList(Activity ctx, String relPath, DataImportCompletedListener completedListener) {
		ImportTask task = new ImportTask(ctx, completedListener);
		task.execute(relPath);
	}

	private static class ImportTask extends ModalTask<String, Integer, Boolean> {
		private DataImportCompletedListener _completedListener;

		public ImportTask(Activity ctx, DataImportCompletedListener completedListener) {
			super(ctx);
			_completedListener = completedListener;
		}

		@Override
		protected Boolean doInBackground(String... params) {
			super.doInBackground(params);

			Context ctx = getContext();
			String relPath = params[0];
			InputStream ins = ctx.getResources().openRawResource(
					ctx.getResources().getIdentifier(relPath,
							"raw", ctx.getPackageName()));

			HotSpotList list = new HotSpotList(Realm.getInstance(ctx));
			list.importList(ins);
			return true;
		}

		@Override
		protected CharSequence getTitle() {
			return "Please Wait...";
		}

		@Override
		protected CharSequence getMessage() {
			return "Importing list";
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);

			_completedListener.onDataImportComplete();
		}
	}

}
