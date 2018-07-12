package suite.primitive;

import static suite.util.Friends.fail;

public interface IntLngSink {

	public void sink2(int c, long f);

	public default IntLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
