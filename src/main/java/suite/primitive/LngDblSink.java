package suite.primitive;

import suite.util.Fail;

public interface LngDblSink {

	public void sink2(long c, double f);

	public default LngDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
