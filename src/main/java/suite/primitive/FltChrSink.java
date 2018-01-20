package suite.primitive;

import suite.util.Fail;

public interface FltChrSink {

	public void sink2(float c, char f);

	public default FltChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
