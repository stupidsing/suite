package suite.primitive;

import suite.util.Fail;

public interface FltFltSink {

	public void sink2(float c, float f);

	public default FltFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
