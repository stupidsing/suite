package suite.file;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

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

	private static int pageSize = 4096;

	private PageFile pageFile;
	private Serializer<V> serializer;

	private static class SerializedPagingException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public SerializedPagingException(IOException ex) {
			super(ex);
		}
	}

	public SerializedPageFile(String filename, Serializer<V> serializer) {
		try {
			this.pageFile = new PageFileImpl(filename, pageSize);
		} catch (FileNotFoundException ex) {
			throw new SerializedPagingException(ex);
		}
		this.serializer = serializer;
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
			return serializer.read(pageFile.load(pageNo));
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

	public void save(int pageNo, V page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			serializer.write(buffer, page);
			pageFile.save(pageNo, buffer);
		} catch (IOException ex) {
			throw new SerializedPagingException(ex);
		}
	}

}
