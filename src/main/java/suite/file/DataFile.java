package suite.file;

import java.io.Closeable;
import java.io.IOException;

import suite.primitive.Bytes;

public interface DataFile<T> extends Closeable {

	public static int defaultPageSize = 4096;

	public void sync() throws IOException;

	public Bytes load(T token) throws IOException;

	public void save(T token, Bytes bytes) throws IOException;

}
