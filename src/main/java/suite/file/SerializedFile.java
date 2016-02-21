package suite.file;

import java.io.Closeable;

public interface SerializedFile<Pointer, V> extends Closeable {

	public void sync();

	public V load(Pointer pointer);

	public void save(Pointer pointer, V value);

}
