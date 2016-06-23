package suite.file;

import java.io.Closeable;
import java.util.List;

import suite.file.ExtentAllocator.Extent;

public interface ExtentFile extends Closeable, DataFile<Extent> {

	public static int blockSize = PageFile.defaultPageSize - 12;

	public List<Extent> scan(int start, int end);

}
