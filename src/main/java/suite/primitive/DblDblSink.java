package suite.primitive;

import suite.util.Fail;

public interface DblDblSink {

	public void sink2(double c, double f);

	public default DblDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
