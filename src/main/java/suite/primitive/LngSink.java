package suite.primitive;

public class LngSink {

	@FunctionalInterface
	public interface LngSink_ {
		public void sink(long c);

		public default LngSink_ rethrow() {
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
	public interface LngObjSink<T> { // extends ObjLongConsumer<T>
		public void sink2(long c, T t);

		public default LngObjSink<T> rethrow() {
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
