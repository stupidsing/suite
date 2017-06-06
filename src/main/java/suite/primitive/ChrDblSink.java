package suite.primitive;

public interface ChrDblSink {

	public void sink2(char c, double f);

	public default ChrDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
