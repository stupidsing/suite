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

	public static <T> void array(T[] from, int fromIndex, T[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void bytes(byte[] from, int fromIndex, byte[] to, int toIndex, int size) {
		primitiveArray(from, fromIndex, to, toIndex, size);
	}

	public static void chars(char[] from, int fromIndex, char[] to, int toIndex, int size) {
		primitiveArray(from, fromIndex, to, toIndex, size);
	}

	public static void floats(float[] from, int fromIndex, float[] to, int toIndex, int size) {
		primitiveArray(from, fromIndex, to, toIndex, size);
	}

	public static void ints(int[] from, int fromIndex, int[] to, int toIndex, int size) {
		primitiveArray(from, fromIndex, to, toIndex, size);
	}

	public static void readerToWriter(Reader in, Writer out) throws IOException {
		try (Reader in_ = in) {
			int len;
			char[] buffer = new char[Constants.bufferSize];
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
			byte[] buffer = new byte[Constants.bufferSize];
			while (0 <= (len = in_.read(buffer))) {
				out.write(buffer, 0, len);
				out.flush();
			}
		}
	}

	private static void primitiveArray(Object from, int fromIndex, Object to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

}
