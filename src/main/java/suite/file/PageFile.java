package suite.file;

import primal.primitive.adt.Bytes;

import java.io.Closeable;

public interface PageFile extends Closeable {

	public static int defaultPageSize = 512;

	public void sync();

	public Bytes load(int pointer);

	public void save(int pointer, Bytes bytes);

}
