package suite.immutable;

import suite.util.FunUtil.Source;

public interface ITree<T> {

	public Source<T> source();

	public T find(T t);

	public ITree<T> add(T t);

	public ITree<T> replace(T t);

	public ITree<T> remove(T t);

}
