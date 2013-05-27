package org.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class IoUtil {

	private static final int bufferSize = 4096;

	public static final Charset charset = Charset.forName("UTF-8");

	public static void moveFile(File from, File to) throws FileNotFoundException, IOException {

		// Serious problem that renameTo do not work across partitions in Linux!
		// We fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

	public static void copyFile(File from, File to) throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(from);
		OutputStream out = new FileOutputStream(to);
		// OutputStream out = new FileOutputStream(f2, true); // Append

		copyStream(in, out);
	}

	public static String readStream(InputStream in) throws IOException {
		byte buffer[] = new byte[bufferSize];
		StringBuilder sb = new StringBuilder();

		while (in.available() > 0) {
			int n = in.read(buffer);
			sb.append(new String(buffer, 0, n, IoUtil.charset));
		}

		return sb.toString();
	}

	public static void writeStream(OutputStream out, String content) throws IOException {
		out.write(content.getBytes(IoUtil.charset));
	}

	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		try {
			int len;
			byte buffer[] = new byte[bufferSize];
			while ((len = in.read(buffer)) > 0)
				out.write(buffer, 0, len);
		} finally {
			in.close();
			out.close();
		}
	}

}
