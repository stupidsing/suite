package suite.util;

import suite.primitive.Ints_;
import suite.primitive.streamlet.IntStreamlet;

public class Streamlet_ {

	public static IntStreamlet forInt(int n) {
		return forInt(0, n);
	}

	public static IntStreamlet forInt(int s, int e) {
		return Ints_.for_(s, e);
	}

}
