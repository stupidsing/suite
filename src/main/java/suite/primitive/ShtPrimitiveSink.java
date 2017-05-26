package suite.primitive;

public class ShtPrimitiveSink {

	@FunctionalInterface
	public interface ShtSink {
		public void sink(short c);
	}

	@FunctionalInterface
	public interface ShtObjSink<T> { // extends ObjShortConsumer<T>
		public void sink2(short c, T t);
	}

}
