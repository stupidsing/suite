package suite.primitive;

import suite.util.Fail;

public interface DblFltSink {

	public void sink2(double c, float f);

	public default DblFltSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
