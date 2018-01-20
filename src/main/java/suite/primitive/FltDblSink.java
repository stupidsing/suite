package suite.primitive;

import suite.util.Fail;

public interface FltDblSink {

	public void sink2(float c, double f);

	public default FltDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
