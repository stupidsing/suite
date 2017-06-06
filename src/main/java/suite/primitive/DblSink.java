package suite.primitive;

public class DblSink {

	@FunctionalInterface
	public interface DblSink_ {
		public void sink(double c);

		public default DblSink_ rethrow() {
			return t -> {
				try {
					sink(t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + t, ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblObjSink<T> { // extends ObjDoubleConsumer<T>
		public void sink2(double c, T t);

		public default DblObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + t, ex);
				}
			};
		}
	}

}
