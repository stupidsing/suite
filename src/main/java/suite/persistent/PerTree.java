package suite.persistent;

import suite.streamlet.Streamlet;

public interface PerTree<T> {

	public Streamlet<T> streamlet();

	public T find(T t);

	public PerTree<T> add(T t);

	public PerTree<T> replace(T t);

	public PerTree<T> remove(T t);

}
