package suite.primitive;

import suite.util.Fail;

public interface ChrChrSink {

	public void sink2(char c, char f);

	public default ChrChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
