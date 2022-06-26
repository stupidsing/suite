package suite.util;

import primal.primitive.IntMoreVerbs.ReadInt;
import primal.streamlet.primitive.IntStreamlet;

public class Streamlet_ {

	public static IntStreamlet forInt(int n) {
		return forInt(0, n);
	}

	public static IntStreamlet forInt(int s, int e) {
		return ReadInt.for_(s, e);
	}

}
