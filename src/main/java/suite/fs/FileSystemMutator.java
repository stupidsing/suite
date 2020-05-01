package suite.fs;

import primal.primitive.adt.Bytes;

import java.util.List;

public interface FileSystemMutator {

	public Bytes read(Bytes name);

	public List<Bytes> list(Bytes start, Bytes end);

	public void replace(Bytes name, Bytes bytes);

	public void replace(Bytes name, int seq, Bytes bytes);

	public void resize(Bytes name, int size1);

}
