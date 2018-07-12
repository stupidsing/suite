package suite.primitive;

import static suite.util.Friends.fail;

public interface LngChrSink {

	public void sink2(long c, char f);

	public default LngChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
