package suite.primitive;

public class PrimitiveSink {

	@FunctionalInterface
	public interface IntIntSink {
		public void sink(int i, int j);
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink(int i, T t);
	}

}
