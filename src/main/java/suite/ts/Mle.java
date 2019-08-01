package suite.ts;

import primal.fp.Funs.Source;
import primal.primitive.DblPrim.DblSource;
import primal.primitive.adt.pair.DblObjPair;

public class Mle {

	public <T extends DblSource> T max(Source<T> source) {
		DblObjPair<T> max = null;

		for (var iter = 0; iter < 10000; iter++) {
			var t = source.g();
			var pair = DblObjPair.of(t.g(), t);
			if (max == null || max.k < pair.k)
				max = pair;
		}

		return max.v;
	}

}
