package suite.algo;

import static suite.util.Friends.forInt;

import suite.adt.map.IntIntMap1;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.DblIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Streamlet;

public class DecisionTree {

	public final Obj_Int<Object[]> classifier;

	private int default_;

	public DecisionTree(Streamlet<IntObjPair<Object[]>> data) {
		default_ = mostFrequent(data);
		classifier = id3(data);
	}

	public Obj_Int<Object[]> id3(Streamlet<IntObjPair<Object[]>> data) {
		var first = data.first();

		if (first == null)
			return fs -> default_;
		else if (data.isAll(datum -> datum.t0 == first.t0))
			return fs -> first.t0;
		else {
			var entropy0 = entropy(data);
			var max = DblIntPair.of(Double.MIN_VALUE, -1);

			forInt(first.t1.length).sink(p -> {
				var es = data //
						.groupBy(datum -> datum.t1[p]) //
						.values() //
						.toDouble(Obj_Dbl.sum(set -> entropy(set) * set.size()));

				var informationGain = entropy0 - es / data.size();

				if (max.t0 < informationGain)
					max.update(informationGain, p);
			});

			var p = max.t1;

			if (0 < p) {
				var funs = data //
						.groupBy(datum -> datum.t1[p], data_ -> data_) //
						.mapValue(this::id3) //
						.toMap();

				return fs -> funs.get(fs[p]).apply(fs);
			} else
				return fs -> mostFrequent(data);
		}
	}

	private int mostFrequent(Streamlet<IntObjPair<Object[]>> data) {
		return data.groupBy(IntObjPair::fst, Streamlet::size).sortByValue(Integer::compareTo).first().t0;
	}

	private double entropy(Iterable<IntObjPair<Object[]>> data) {
		var hist0 = new IntIntMap1();
		for (var datum : data)
			hist0.update(datum.t0, v -> (v != IntFunUtil.EMPTYVALUE ? v : 0) + 1);
		var hist1 = hist0.values();
		var sum = (double) hist1.sum();
		return hist1.toDouble(Int_Dbl.sum(c -> Math.log(c / sum)));
	}

}
