package suite.primitive;

public class ChrSink {

	@FunctionalInterface
	public interface ChrSink_ {
		public void sink(char c);

		public default ChrSink_ rethrow() {
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
	public interface ChrObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(char c, T t);

		public default ChrObjSink<T> rethrow() {
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
