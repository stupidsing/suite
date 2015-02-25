package suite.file;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;

import suite.primitive.Bytes;
import suite.util.SerializeUtil.Serializer;

/**
 * Persists B-tree pages to file on disk.
 *
 * The following must holds when using this class:
 *
 * pageSize >= sizeof(char) + 2 * sizeof(int) + branchFactor * branchPointerSize
 *
 * where branchPointerSize = max(sizeof(int), sizeof(Value))
 */
public class SerializedPageFile<V> implements Closeable {

	private PageFile pageFile;
	private Serializer<V> serializer;

	private static class SerializedPagingException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public SerializedPagingException(IOException ex) {
			super(ex);
		}
	}

	public SerializedPageFile(PageFile pageFile, Serializer<V> serializer) {
		this.pageFile = pageFile;
		this.serializer = serializer;
	}

	@Override
	public void close() {
		try {
			pageFile.close();
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

	public void sync() throws IOException {
		pageFile.sync();
	}

	public V load(int pageNo) {
		try {
			return serializer.read(new DataInputStream(pageFile.load(pageNo).asInputStream()));
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

	public void save(int pageNo, V page) {
		try {
			pageFile.save(pageNo, Bytes.of(dataOutput -> serializer.write(dataOutput, page)));
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}
}
