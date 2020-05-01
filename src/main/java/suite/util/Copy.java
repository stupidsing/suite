package suite.util;

import primal.Nouns.Buffer;
import primal.Verbs.Equals;
import primal.Verbs.New;
import primal.Verbs.Th;

import java.io.*;
import java.net.SocketException;

import static primal.statics.Fail.fail;

public class Copy {

	public static void readerToWriter(Reader in, Writer out) throws IOException {
		try (var in_ = in) {
			int len;
			var buffer = new char[Buffer.size];
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
		return streamByThread(is, os, true);
	}

	public static Th streamByThread(InputStream is, OutputStream os, boolean isClose) {
		return New.thread(() -> {
			try (var is_ = is) {
				stream_(is_, os);
			} catch (InterruptedIOException ex) {
			} catch (SocketException ex) {
				if (!Equals.string(ex.getMessage(), "Socket closed"))
					throw ex;
			} finally {
				if (isClose)
					os.close();
			}
		});
	}

	public static void stream(InputStream is, OutputStream os) {
		try (var is_ = is) {
			stream_(is_, os);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	private static void stream_(InputStream is, OutputStream os) throws IOException {
		var buffer = new byte[Buffer.size];
		int len;
		while (0 <= (len = is.read(buffer))) {
			os.write(buffer, 0, len);
			os.flush();
		}
	}

}
