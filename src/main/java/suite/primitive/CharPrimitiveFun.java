package suite.primitive;

public class CharPrimitiveFun {

	@FunctionalInterface
	public interface Char_Char {
		public char apply(char c);
	}

	@FunctionalInterface
	public interface CharChar_Obj<T> {
		public T apply(char c, int j);
	}

	@FunctionalInterface
	public interface CharObj_Char<T> {
		public char apply(char i, T t);
	}

	@FunctionalInterface
	public interface CharObj_Obj<X, Y> {
		public Y apply(char c, X x);
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
