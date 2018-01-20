package suite.primitive;

import suite.util.Fail;

public interface DblChrSink {

	public void sink2(double c, char f);

	public default DblChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
