package suite.primitive;

public class ChrPrimitiveSink {

	@FunctionalInterface
	public interface ChrSink {
		public void sink(char c);
	}

	@FunctionalInterface
	public interface ChrObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(char c, T t);
	}

}
