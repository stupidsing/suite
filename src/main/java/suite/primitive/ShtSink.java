package suite.primitive;

public class ShtSink {

	@FunctionalInterface
	public interface ShtSink_ {
		public void sink(short c);
	}

	@FunctionalInterface
	public interface ShtObjSink<T> { // extends ObjShortConsumer<T>
		public void sink2(short c, T t);
	}

}
