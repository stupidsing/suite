package suite.primitive;

public class IntSink {

	@FunctionalInterface
	public interface IntSink_ {
		public void sink(int c);
	}

	@FunctionalInterface
	public interface IntObjSink<T> { // extends ObjIntConsumer<T>
		public void sink2(int c, T t);
	}

}
