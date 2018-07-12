package suite.primitive;

import static suite.util.Friends.fail;

public interface LngDblSink {

	public void sink2(long c, double f);

	public default LngDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
