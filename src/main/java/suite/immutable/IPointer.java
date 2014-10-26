package suite.immutable;

public interface IPointer<T> {

	/**
	 * @return current element, if null if end of list was reached.
	 */
	public T head();

	/**
	 * @return pointer pointing to succeeding element.
	 */
	public IPointer<T> tail();

}
