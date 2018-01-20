package suite.primitive;

import suite.util.Fail;

public interface ChrDblSink {

	public void sink2(char c, double f);

	public default ChrDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
