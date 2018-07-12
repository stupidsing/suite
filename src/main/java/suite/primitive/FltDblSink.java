package suite.primitive;

import static suite.util.Friends.fail;

public interface FltDblSink {

	public void sink2(float c, double f);

	public default FltDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
