package suite.algo;

import static suite.util.Friends.forInt;

import java.util.function.Predicate;

import suite.adt.map.IntIntMap1;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class DecisionTree {

	public Obj_Int<boolean[]> id3(Streamlet<IntObjPair<boolean[]>> data, int default_) {
		if (data.first() == null)
			return fs -> default_;
		else if (isSingleClassification(data) != null)
			return fs -> isSingleClassification(data);
		else {
			var entropy0 = entropy(data);
			var max = DblObjPair.<Predicate<boolean[]>> of(Double.MIN_VALUE, null);

			forInt(data.first().t1.length).sink(p -> {
				Predicate<boolean[]> pred = datum -> datum[p];
				var sets = data //
						.partition(datum -> pred.test(datum.t1)) //
						.map((s0, s1) -> Read.each(s0.collect(), s1.collect()));

				var es = sets.toDouble(Obj_Dbl.sum(set -> entropy(set) * set.size()));
				var informationGain = entropy0 - es / data.size();

				if (max.t0 < informationGain)
					max.update(informationGain, pred);
			});

			var pred = max.t1;
			var partitions = data.partition(datum -> pred.test(datum.t1));
			var f0 = id3(partitions.t0, default_);
			var f1 = id3(partitions.t1, default_);
			return fs -> (pred.test(fs) ? f0 : f1).apply(fs);
		}
	}

	private double entropy(Streamlet<IntObjPair<boolean[]>> data) {
		var hist0 = new IntIntMap1();
		for (var datum : data)
			hist0.update(datum.t0, v -> (v != IntFunUtil.EMPTYVALUE ? v : 0) + 1);
		var hist1 = hist0.values();
		var sum = (double) hist1.sum();
		return hist1.toDouble(Int_Dbl.sum(c -> Math.log(c / sum)));
	}

	private Integer isSingleClassification(Streamlet<IntObjPair<boolean[]>> data) {
		var b = true;
		var cl = data.first().t0;
		for (var datum : data)
			b &= datum.t0 == cl;
		return b ? cl : null;
	}

}
