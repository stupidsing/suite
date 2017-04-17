package suite.primitive;

public class PrimitiveSink {

	@FunctionalInterface
	public interface IntIntSink2 {
		public void sink2(int i, int j);
	}

	@FunctionalInterface
	public interface IntObjSink2<T> { // extends ObjIntConsumer<T>
		public void sink2(int i, T t);
	}

}
