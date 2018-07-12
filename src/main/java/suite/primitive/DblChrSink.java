package suite.primitive;

import static suite.util.Friends.fail;

public interface DblChrSink {

	public void sink2(double c, char f);

	public default DblChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
