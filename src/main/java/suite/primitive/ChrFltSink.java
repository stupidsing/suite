package suite.primitive;

import suite.util.Fail;

public interface ChrFltSink {

	public void sink2(char c, float f);

	public default ChrFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
