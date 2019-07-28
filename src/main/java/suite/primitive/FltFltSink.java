package suite.primitive;

import static suite.util.Fail.fail;

public interface FltFltSink {

	public void sink2(float c, float f);

	public default FltFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
