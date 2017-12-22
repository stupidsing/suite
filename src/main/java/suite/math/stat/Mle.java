package suite.math.stat;

import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.adt.pair.DblObjPair;
import suite.util.FunUtil.Source;

public class Mle {

	public <T extends DblSource> T max(Source<T> source) {
		DblObjPair<T> max = DblObjPair.of(Double.MIN_VALUE, null);

		for (int b = 0; b < 10000; b++) {
			T t = source.source();
			DblObjPair<T> pair = DblObjPair.of(t.source(), t);
			if (max == null || max.t0 < pair.t0)
				max = pair;
		}

		return max.t1;
	}

}
