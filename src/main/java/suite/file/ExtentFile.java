package suite.file;

import java.io.Closeable;
import java.util.List;

import primal.primitive.adt.Bytes;
import suite.file.ExtentAllocator.Extent;

public interface ExtentFile extends Closeable {

	public static int blockSize = PageFile.defaultPageSize - 12;

	public void sync();

	public Bytes load(Extent extent);

	public void save(Extent extent, Bytes bytes);

	public List<Extent> scan(int start, int end);

}
