package suite.primitive;

import static suite.util.Friends.fail;

public interface DblFltSink {

	public void sink2(double c, float f);

	public default DblFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
