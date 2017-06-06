package suite.primitive;

public interface DblChrSink {

	public void sink2(double c, char f);

	public default DblChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
