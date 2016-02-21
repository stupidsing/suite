package suite.fs;

import java.io.Closeable;

public interface FileSystem extends Closeable {

	public void create();

	public FileSystemMutator mutate();

}
