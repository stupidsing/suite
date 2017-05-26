package suite.primitive;

public class FltSink {

	@FunctionalInterface
	public interface FltSink_ {
		public void sink(float c);
	}

	@FunctionalInterface
	public interface FltObjSink<T> { // extends ObjFloatConsumer<T>
		public void sink2(float c, T t);
	}

}
