package suite.primitive;

public class IntPrimitiveSink {

	@FunctionalInterface
	public interface IntSink {
		public void sink(int c);
	}

	@FunctionalInterface
	public interface IntIntSink {
		public void sink2(int c, int d);
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink2(int c, T t);
	}

}
