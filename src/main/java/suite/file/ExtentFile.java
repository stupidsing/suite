package suite.file;

import java.io.Closeable;
import java.util.List;

import suite.file.ExtentAllocator.Extent;
import suite.primitive.Bytes;

public interface ExtentFile extends Closeable {

	public static int blockSize = PageFile.defaultPageSize - 12;

	public void sync();

	public Bytes load(Extent extent);

	public void save(Extent extent, Bytes bytes);

	public List<Extent> scan(int start, int end);

}
