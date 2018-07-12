package suite.primitive;

import static suite.util.Friends.fail;

public interface FltChrSink {

	public void sink2(float c, char f);

	public default FltChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
