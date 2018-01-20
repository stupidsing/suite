package suite.primitive;

import suite.util.Fail;

public interface IntIntSink {

	public void sink2(int c, int f);

	public default IntIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
