package suite.ts;

import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.FunUtil.Source;

public class Mle {

	public <T extends DblSource> T max(Source<T> source) {
		DblObjPair<T> max = null;

		for (var iter = 0; iter < 10000; iter++) {
			var t = source.source();
			var pair = DblObjPair.of(t.source(), t);
			if (max == null || max.t0 < pair.t0)
				max = pair;
		}

		return max.t1;
	}

}
