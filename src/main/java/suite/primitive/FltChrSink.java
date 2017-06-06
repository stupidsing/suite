package suite.primitive;

public interface FltChrSink {

	public void sink2(float c, char f);

	public default FltChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
