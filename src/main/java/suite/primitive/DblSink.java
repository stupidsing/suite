package suite.primitive;

public class DblSink {

	@FunctionalInterface
	public interface DblSink_ {
		public void sink(double c);
	}

	@FunctionalInterface
	public interface DblObjSink<T> { // extends ObjDoubleConsumer<T>
		public void sink2(double c, T t);
	}

}
