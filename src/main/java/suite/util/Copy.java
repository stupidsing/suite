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
		try (Reader in_ = in) {
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(clonee);
		out.flush();
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		@SuppressWarnings("unchecked")
		T cloned = (T) in.readObject();
		return cloned;
	}

	public static Thread streamByThread(InputStream is, OutputStream os) {
		return Thread_.newThread(() -> {
			try (InputStream is_ = is; OutputStream os_ = os) {
				stream(is_, os_);
			} catch (InterruptedIOException ex) {
			}
		});
	}

	public static void stream(InputStream in, OutputStream out) throws IOException {
		try (InputStream in_ = in) {
			int len;
			var buffer = new byte[Constants.bufferSize];
			while (0 <= (len = in_.read(buffer))) {
				out.write(buffer, 0, len);
				out.flush();
			}
		}
	}

}
