package suite.fs;

import java.io.Closeable;
import java.io.IOException;

public interface FileSystem extends Closeable {

	public void create() throws IOException;

	public FileSystemMutator mutate();

}
