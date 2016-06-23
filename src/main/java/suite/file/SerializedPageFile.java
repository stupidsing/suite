package suite.file;

import java.io.Closeable;

public interface SerializedPageFile<V> extends Closeable {

	public void sync();

	public V load(int pointer);

	public void save(int pointer, V value);

}
