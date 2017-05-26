package suite.primitive;

public class FltPrimitiveSink {

	@FunctionalInterface
	public interface FltSink {
		public void sink(float c);
	}

	@FunctionalInterface
	public interface FltObjSink<T> { // extends ObjFloatConsumer<T>
		public void sink2(float c, T t);
	}

}
