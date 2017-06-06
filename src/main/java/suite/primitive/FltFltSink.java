package suite.primitive;

public interface FltFltSink {

	public void sink2(float c, float f);

	public default FltFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
