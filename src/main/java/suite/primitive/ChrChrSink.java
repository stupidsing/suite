package suite.primitive;

public interface ChrChrSink {

	public void sink2(char c, char f);

	public default ChrChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
