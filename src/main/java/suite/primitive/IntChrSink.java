package suite.primitive;

import suite.util.Fail;

public interface IntChrSink {

	public void sink2(int c, char f);

	public default IntChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				Fail.t("for key " + k, ex);
			}
		};
	}

}
