package suite.file;

import java.io.Closeable;

public interface PageFile extends Closeable, DataFile<Integer> {

	public static int defaultPageSize = 4096;

}
