package suite.immutable.btree;

import java.io.Closeable;
import java.util.List;

import suite.primitive.Bytes;

public interface FileSystem extends Closeable {

	public void create();

	public Bytes read(Bytes name);

	public List<Bytes> list(Bytes start, Bytes end);

	public void replace(Bytes name, Bytes bytes);

	public void replace(Bytes name, int seq, Bytes bytes);

	public void resize(Bytes name, int size1);

}
