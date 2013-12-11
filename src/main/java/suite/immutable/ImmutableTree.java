package suite.immutable;

public interface ImmutableTree<T> extends Iterable<T> {

	public T find(T t);

	public ImmutableTree<T> add(T t);

	public ImmutableTree<T> replace(T t);

	public ImmutableTree<T> remove(T t);

}
