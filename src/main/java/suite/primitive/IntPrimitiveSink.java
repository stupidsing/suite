package suite.primitive;

public class IntPrimitiveSink {

	@FunctionalInterface
	public interface IntSink {
		public void sink(int i);
	}

	@FunctionalInterface
	public interface IntIntSink {
		public void sink(int i, int j);
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink(int i, T t);
	}

}
