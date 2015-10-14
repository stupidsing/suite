package suite.file;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

public class SerializedPageFilePersister<V> implements Closeable {

	private AtomicInteger nPages = new AtomicInteger(1);
	private SerializedPageFile<V> pageFile;

	public SerializedPageFilePersister(SerializedPageFile<V> pageFile) {
		this.pageFile = pageFile;
	}

	@Override
	public void close() {
		pageFile.close();
	}

	public V load(int token) {
		return pageFile.load(token);
	}

	public int save(V v) {
		int token = nPages.incrementAndGet();
		pageFile.save(token, v);
		return token;
	}

}
