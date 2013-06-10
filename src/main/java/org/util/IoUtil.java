package org.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class IoUtil {

	private static final int bufferSize = 4096;

	public static final Charset charset = Charset.forName("UTF-8");

	public static void moveFile(File from, File to) throws IOException {

		// Serious problem that renameTo do not work across partitions in Linux!
		// We fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

	public static void copyFile(File from, File to) throws IOException {
		copy(new FileInputStream(from), new FileOutputStream(to));
		// new FileOutputStream(f2, true); // Append
	}

	public static String readStream(InputStream in) throws IOException {
		try (InputStream in_ = in) {
			char buffer[] = new char[bufferSize];
			StringBuilder sb = new StringBuilder();
			InputStreamReader reader = new InputStreamReader(in_);

			while (reader.ready()) {
				int n = reader.read(buffer);
				sb.append(new String(buffer, 0, n));
			}

			return sb.toString();
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		try (InputStream in_ = in; OutputStream out_ = out) {
			int len;
			byte buffer[] = new byte[bufferSize];
			while ((len = in_.read(buffer)) > 0)
				out_.write(buffer, 0, len);
		}
	}

}
