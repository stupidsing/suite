package suite.immutable;

import suite.util.FunUtil.Source;

public interface ImmutableTree<T> {

	public Source<T> source();

	public T find(T t);

	public ImmutableTree<T> add(T t);

	public ImmutableTree<T> replace(T t);

	public ImmutableTree<T> remove(T t);

}
