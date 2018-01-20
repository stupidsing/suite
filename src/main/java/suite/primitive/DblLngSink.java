package suite.primitive;

import suite.util.Fail;

public interface DblLngSink {

	public void sink2(double c, long f);

	public default DblLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
