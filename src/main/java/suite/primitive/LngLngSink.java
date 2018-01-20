package suite.primitive;

import suite.util.Fail;

public interface LngLngSink {

	public void sink2(long c, long f);

	public default LngLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
