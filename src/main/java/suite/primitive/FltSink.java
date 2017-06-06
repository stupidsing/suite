package suite.primitive;

public class FltSink {

	@FunctionalInterface
	public interface FltSink_ {
		public void sink(float c);

		public default FltSink_ rethrow() {
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
	public interface FltObjSink<T> { // extends ObjFloatConsumer<T>
		public void sink2(float c, T t);

		public default FltObjSink<T> rethrow() {
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
