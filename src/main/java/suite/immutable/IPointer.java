package suite.immutable;

public interface IPointer<T> {

	public T head();

	public IPointer<T> tail();

}
