package suite.primitive;

public class LngSink {

	@FunctionalInterface
	public interface LngSink_ {
		public void sink(long c);
	}

	@FunctionalInterface
	public interface LngObjSink<T> { // extends ObjLongConsumer<T>
		public void sink2(long c, T t);
	}

}
