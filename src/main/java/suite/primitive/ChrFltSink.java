package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrFltSink {

	public void sink2(char c, float f);

	public default ChrFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
