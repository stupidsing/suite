package suite.file;

import java.io.Closeable;
import java.io.IOException;

import suite.primitive.Bytes;

public interface DataFile<Pointer> extends Closeable {

	public static int defaultPageSize = 4096;

	public void sync() throws IOException;

	public Bytes load(Pointer pointer) throws IOException;

	public void save(Pointer pointer, Bytes bytes) throws IOException;

}
