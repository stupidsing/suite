package suite.file;

import java.io.Closeable;

import suite.primitive.Bytes;

public interface DataFile<Pointer> extends Closeable {

	public static int defaultPageSize = 4096;

	public void sync();

	public Bytes load(Pointer pointer);

	public void save(Pointer pointer, Bytes bytes);

}
