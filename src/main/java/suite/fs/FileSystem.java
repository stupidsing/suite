package suite.fs;

import java.io.Closeable;

public interface FileSystem extends Closeable {

	public FileSystemMutator mutate();

}
