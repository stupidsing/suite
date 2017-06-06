package suite.primitive;

public interface DblFltSink {

	public void sink2(double c, float f);

	public default DblFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
