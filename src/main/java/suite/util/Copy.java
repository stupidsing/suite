package suite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import suite.Constants;

public class Copy {

	public static void readerToWriter(Reader in, Writer out) throws IOException {
		try (var in_ = in) {
			int len;
			var buffer = new char[Constants.bufferSize];
			while (0 <= (len = in_.read(buffer)))
				out.write(buffer, 0, len);
		}
	}

	/**
	 * Clones slowly by serializing and de-serializing.
	 */
	public static <T> T serializable(T clonee) throws IOException, ClassNotFoundException {
		byte[] bs;

		try (var baos = new ByteArrayOutputStream(); var out = new ObjectOutputStream(baos);) {
			out.writeObject(clonee);
			out.flush();
			bs = baos.toByteArray();
		}

		try (var bais = new ByteArrayInputStream(bs); var in = new ObjectInputStream(bais);) {
			@SuppressWarnings("unchecked")
			var cloned = (T) in.readObject();
			return cloned;
		}
	}

	public static Th streamByThread(InputStream is, OutputStream os) {
		return Thread_.newThread(() -> {
			try (var is_ = is; var os_ = os) {
				stream(is_, os_);
			} catch (InterruptedIOException ex) {
			}
		});
	}

	public static void stream(InputStream in, OutputStream out) throws IOException {
		try (var in_ = in) {
			var buffer = new byte[Constants.bufferSize];
			int len;
			while (0 <= (len = in_.read(buffer))) {
				out.write(buffer, 0, len);
				out.flush();
			}
		}
	}

}
