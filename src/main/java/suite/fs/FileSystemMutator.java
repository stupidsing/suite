package suite.fs;

import java.io.IOException;
import java.util.List;

import suite.primitive.Bytes;

public interface FileSystemMutator {

	public Bytes read(Bytes name) throws IOException;

	public List<Bytes> list(Bytes start, Bytes end) throws IOException;

	public void replace(Bytes name, Bytes bytes) throws IOException;

	public void replace(Bytes name, int seq, Bytes bytes) throws IOException;

	public void resize(Bytes name, int size1) throws IOException;

}
