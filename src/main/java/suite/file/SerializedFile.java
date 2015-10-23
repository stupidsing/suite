package suite.file;

import java.io.Closeable;
import java.io.IOException;

public interface SerializedFile<Pointer, V> extends Closeable {

	public void sync() throws IOException;

	public V load(Pointer pointer) throws IOException;

	public void save(Pointer pointer, V value) throws IOException;

}
