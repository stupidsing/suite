package suite.math.stat;

import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.adt.pair.DblObjPair;
import suite.util.FunUtil.Source;

public class Mle {

	public <T extends DblSource> T max(Source<T> source) {
		DblObjPair<T> max = null;

		for (int iter = 0; iter < 10000; iter++) {
			T t = source.source();
			DblObjPair<T> pair = DblObjPair.of(t.source(), t);
			if (max == null || max.t0 < pair.t0)
				max = pair;
		}

		return max.t1;
	}

}
