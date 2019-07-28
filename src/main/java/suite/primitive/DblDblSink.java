package suite.primitive;

import static suite.util.Fail.fail;

public interface DblDblSink {

	public void sink2(double c, double f);

	public default DblDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
