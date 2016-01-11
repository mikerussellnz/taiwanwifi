package com.mikerussellnz.taiwanwifi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by mike on 1/12/15.
 */
public class HotSpotList {
	private Realm _realm;

	public String stripQuotes(String value) {
		if (value == null) {
			return null;
		}
		int start = 0;
		int end = value.length() - 1;
		if (value.charAt(start) == '"') {
			start++;
		}
		if (value.charAt(end) == '"') {
			end--;
		}
		String result = value.substring(start, end + 1);
		return result;
	}

	public HotSpotList(Realm realm) {
		_realm = realm;
	}

	public int count() {
		return all().size();
	}

	public RealmResults<HotSpotModel> all() {
		// TODO: support chinese.
		//String language = Locale.getDefault().getLanguage();
		//return _realm.where(HotSpotModel.class).equalTo("language", language).findAll();
		return _realm.allObjects(HotSpotModel.class);
	}

	public void clear() {
		_realm.beginTransaction();
		_realm.clear(HotSpotModel.class);
		_realm.commitTransaction();
	}

	public void importList(InputStream input) {
		System.out.println("reading list");

		InputStreamReader inputreader = new InputStreamReader(input);
		BufferedReader buffreader = new BufferedReader(inputreader);

		_realm.beginTransaction();

		String line = null;
		boolean headers = true;
		do {
			try {
				line = buffreader.readLine();
				if (!headers && line != null) {
					String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					double lat = Double.parseDouble(stripQuotes(parts[4]));
					double lon = Double.parseDouble(stripQuotes(parts[5]));
					String name = stripQuotes(parts[2]);
					String address = stripQuotes(parts[3]);

					HotSpotModel fields = new HotSpotModel();
					fields.setLat(lat);
					fields.setLon(lon);
					fields.setName(name);
					fields.setAddress(address);
					_realm.copyToRealm(fields);
				}
				headers = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (line != null);

		_realm.commitTransaction();
		System.out.println("done reading list");
	}
}
