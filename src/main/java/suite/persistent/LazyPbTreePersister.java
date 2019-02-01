package suite.persistent;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface LazyPbTreePersister<P, T> extends Closeable {

	public LazyPbTree<T> load(P pointer);

	public P save(LazyPbTree<T> tree);

	public Map<P, P> gc(List<P> pointers, int back);

}
