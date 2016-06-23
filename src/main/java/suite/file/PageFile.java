package suite.file;

import java.io.Closeable;

import suite.primitive.Bytes;

public interface PageFile extends Closeable {

	public static int defaultPageSize = 512;

	public void sync();

	public Bytes load(int pointer);

	public void save(int pointer, Bytes bytes);

}
