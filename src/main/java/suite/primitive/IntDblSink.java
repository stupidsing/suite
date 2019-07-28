package suite.primitive;

import static suite.util.Fail.fail;

public interface IntDblSink {

	public void sink2(int c, double f);

	public default IntDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
