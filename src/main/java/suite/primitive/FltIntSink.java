package suite.primitive;

import suite.util.Fail;

public interface FltIntSink {

	public void sink2(float c, int f);

	public default FltIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
