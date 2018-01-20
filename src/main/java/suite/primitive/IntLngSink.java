package suite.primitive;

import suite.util.Fail;

public interface IntLngSink {

	public void sink2(int c, long f);

	public default IntLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
