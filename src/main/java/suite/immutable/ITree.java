package suite.immutable;

import suite.streamlet.Streamlet;

public interface ITree<T> {

	public Streamlet<T> streamlet();

	public T find(T t);

	public ITree<T> add(T t);

	public ITree<T> replace(T t);

	public ITree<T> remove(T t);

}
