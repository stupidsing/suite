package suite.primitive;

public class ShtPredicate {

	@FunctionalInterface
	public interface ShtPredicate_ {
		public boolean test(short c);
	}

	@FunctionalInterface
	public interface ShtObjPredicate<T> {
		public boolean test(short c, T t);
	}

}
