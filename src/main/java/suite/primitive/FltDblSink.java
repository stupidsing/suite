package suite.primitive;

import static primal.statics.Fail.fail;

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
