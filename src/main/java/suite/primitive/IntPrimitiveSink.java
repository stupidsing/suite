package suite.primitive;

public class IntPrimitiveSink {

	@FunctionalInterface
	public interface IntSink {
		public void sink(int c);
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink2(int c, T t);
	}

}
