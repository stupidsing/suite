package suite.persistent;

public interface PerPointer<T> {

	/**
	 * @return current element, if null if end of list was reached.
	 */
	public T head();

	/**
	 * @return pointer pointing to succeeding element.
	 */
	public PerPointer<T> tail();

}
