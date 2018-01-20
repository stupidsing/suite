package suite.primitive;

import suite.util.Fail;

public interface ChrIntSink {

	public void sink2(char c, int f);

	public default ChrIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
