package suite.streamlet;

import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrSource;
import primal.puller.primitive.ChrPuller;
import primal.streamlet.primitive.ChrStreamlet;

public class ReadChars {

	public static ChrStreamlet from(CharSequence s) {
		return new ChrStreamlet(() -> ChrPuller.of(new ChrSource() {
			private int index = 0;

			public char g() {
				return index < s.length() ? s.charAt(index++) : ChrPrim.EMPTYVALUE;
			}
		}));
	}

}
