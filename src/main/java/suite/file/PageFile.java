package suite.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface PageFile extends Closeable {

	public void sync() throws IOException;

	public ByteBuffer load(int pageNo) throws IOException;

	public void save(int pageNo, ByteBuffer buffer) throws IOException;

}
