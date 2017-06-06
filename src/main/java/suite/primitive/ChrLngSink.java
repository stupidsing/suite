package suite.primitive;

public interface ChrLngSink {

	public void sink2(char c, long f);

	public default ChrLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
