package suite.primitive;

import suite.util.Fail;

public interface LngChrSink {

	public void sink2(long c, char f);

	public default LngChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
