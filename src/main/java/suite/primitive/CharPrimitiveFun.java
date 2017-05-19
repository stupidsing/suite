package suite.primitive;

public class CharPrimitiveFun {

	@FunctionalInterface
	public interface Char_Char {
		public int apply(int i);
	}

	@FunctionalInterface
	public interface CharChar_Obj<T> {
		public T apply(int i, int j);
	}

	@FunctionalInterface
	public interface CharObj_Obj<X, Y> {
		public Y apply(int i, X x);
	}

	@FunctionalInterface
	public interface Char_Obj<T> {
		T apply(char c);
	}

	@FunctionalInterface
	public interface Obj_Char<T> {
		char apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Char<X, Y> {
		char apply(X x, Y y);
	}

}
