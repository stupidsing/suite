package suite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class Copy {

	private static final int bufferSize = 4096;

	public static <T> void array(T from[], int fromIndex, T to[], int toIndex, int size) {
		if (size != 0)
			System.arraycopy(from, fromIndex, to, toIndex, size);
	}

	public static void primitiveArray(Object from, int fromIndex, Object to, int toIndex, int size) {
		if (size != 0)
			System.arraycopy(from, fromIndex, to, toIndex, size);
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

	public static void readerToWriter(Reader in, Writer out) throws IOException {
		try (Reader in_ = in) {
			int len;
			char buffer[] = new char[bufferSize];
			while ((len = in_.read(buffer)) >= 0)
				out.write(buffer, 0, len);
		}
	}

	public static void stream(InputStream in, OutputStream out) throws IOException {
		try (InputStream in_ = in) {
			int len;
			byte buffer[] = new byte[bufferSize];
			while ((len = in_.read(buffer)) >= 0)
				out.write(buffer, 0, len);
		}
	}

}
