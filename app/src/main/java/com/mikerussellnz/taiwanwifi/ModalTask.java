package com.mikerussellnz.taiwanwifi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class ModalTask<T,U,V> extends AsyncTask<T,U,V> {
	private static boolean _progressDialogBeingDisplayed = false;

	private Activity _ctx;
	private ProgressDialog _progressDialog;

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// for chaining, onPreExecute is executed before the task blocks on the queue.
		// If another task is running we don't want to put our progress dialog over it
		// until we are dequeued.
		if (!_progressDialogBeingDisplayed) {
			showProgressDialog();
		}
	}

	@Override
	protected V doInBackground(T... params) {
		if (_progressDialog == null) {
			_ctx.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showProgressDialog();

				}
			});
		}
		return null;
	}

	private void showProgressDialog() {
		_progressDialog.setTitle(getTitle());
		_progressDialog.setMessage(getMessage());
		_progressDialog.setIndeterminate(true);
		_progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		_progressDialog.show();

		_progressDialogBeingDisplayed = true;
	}

	public ModalTask(Activity ctx) {
		_ctx = ctx;
		_progressDialog = new ProgressDialog(ctx);
	}

	protected abstract CharSequence getTitle();

	protected abstract CharSequence getMessage();

	protected Context getContext() { return _ctx; }

	@Override
	protected void onPostExecute(V v) {
		super.onPostExecute(v);
		_progressDialog.hide();

		_progressDialogBeingDisplayed = false;
	}
}
