package suite.primitive;

public interface IntFltSink {

	public void sink2(int c, float f);

	public default IntFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
