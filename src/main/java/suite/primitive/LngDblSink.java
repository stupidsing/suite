package suite.primitive;

public interface LngDblSink {

	public void sink2(long c, double f);

	public default LngDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
