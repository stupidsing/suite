package suite.immutable;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface LazyIbTreePersister<P, T> extends Closeable {

	public LazyIbTree<T> load(P pointer);

	public P save(LazyIbTree<T> tree);

	public Map<P, P> gc(List<P> pointers, int back);

}
