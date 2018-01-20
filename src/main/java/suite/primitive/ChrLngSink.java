package suite.primitive;

import suite.util.Fail;

public interface ChrLngSink {

	public void sink2(char c, long f);

	public default ChrLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
