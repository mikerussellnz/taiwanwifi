package com.mikerussellnz.taiwanwifi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mike on 23/12/15.
 */
public class IOUtils {
	public static long copyStream(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[32768];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}
}
