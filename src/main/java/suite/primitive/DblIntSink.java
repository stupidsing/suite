package suite.primitive;

public interface DblIntSink {

	public void sink2(double c, int f);

	public default DblIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
