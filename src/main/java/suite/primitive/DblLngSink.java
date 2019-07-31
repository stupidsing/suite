package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblLngSink {

	public void sink2(double c, long f);

	public default DblLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
