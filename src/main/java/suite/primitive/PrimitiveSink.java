package suite.primitive;

public class PrimitiveSink {

	@FunctionalInterface
	public interface IntIntSink2 {
		public void sink2(int i, int j);
	}

	@FunctionalInterface
	public interface IntObjSink2<T> {
		public void sink2(int i, T t);
	}

	@FunctionalInterface
	public interface ObjIntSink2<T> {
		public void sink2(T t, int i);
	}

}
