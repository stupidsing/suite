package suite.file;

import java.io.Closeable;
import java.io.IOException;

import suite.primitive.Bytes;

public interface PageFile extends Closeable {

	public static int defaultPageSize = 4096;

	public void sync() throws IOException;

	public Bytes load(int pageNo) throws IOException;

	public void save(int pageNo, Bytes bytes) throws IOException;

}
