package suite.file;

import java.io.Closeable;

import suite.file.ExtentAllocator.Extent;

public interface ExtentFile extends Closeable, DataFile<Extent> {

}
