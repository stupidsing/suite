package suite.algo;

import static suite.util.Streamlet_.forInt;

import java.util.Random;

import suite.adt.map.IntIntMap1;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.DblIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Streamlet;

public class DecisionTree {

	private Random random = new Random();

	public Classifier bag(Streamlet<IntObjPair<Object[]>> input, int n, int nr) { // bootstrap aggregating
		var list = input.toList();
		var size = list.size();

		var classifyList = forInt(n).map(i -> {
			var input_ = forInt(nr).map(ir -> list.get(random.nextInt(size))).collect();
			return of(input_).classify;
		}).collect();

		Obj_Int<Object[]> classify = xs -> classifyList //
				.map(cl -> cl.apply(xs)) //
				.groupBy(y -> y, Streamlet::size) //
				.sortByValue(Integer::compareTo) //
				.first().k;

		return new Classifier(input, classify);
	}

	public Classifier of(Streamlet<IntObjPair<Object[]>> input) {
		var default_ = majority(input);

		var object = new Object() {
			private Obj_Int<Object[]> id3(Streamlet<IntObjPair<Object[]>> data) {
				var first = data.first();

				if (first == null)
					return xs -> default_;
				else if (data.isAll(datum -> datum.k == first.k))
					return xs -> first.k;
				else {
					var entropy0 = entropy(data);
					var max = DblIntPair.of(Double.MIN_VALUE, -1);

					forInt(first.v.length).sink(p -> {
						var es = data //
								.groupBy(datum -> datum.v[p]) //
								.values() //
								.toDouble(Obj_Dbl.sum(set -> entropy(set) * set.size()));

						var informationGain = entropy0 - es / data.size();

						if (max.t0 < informationGain)
							max.update(informationGain, p);
					});

					return max.map((informationGain, p) -> {
						if (0 < informationGain) {
							var funs = data //
									.groupBy(datum -> datum.v[p], data_ -> data_) //
									.mapValue(this::id3) //
									.toMap();

							return xs -> funs.get(xs[p]).apply(xs);
						} else {
							var y = majority(data);
							return xs -> y;
						}
					});
				}
			}
		};

		return new Classifier(input, object.id3(input));
	}

	public class Classifier {
		public final Streamlet<IntObjPair<Object[]>> input;
		public final Obj_Int<Object[]> classify;

		private Classifier(Streamlet<IntObjPair<Object[]>> input, Obj_Int<Object[]> classify) {
			this.input = input;
			this.classify = classify;
		}

		public double error() {
			var correct = input.toInt(Obj_Int.sum(datum -> datum.map((y, xs) -> classify.apply(xs) == y ? 1 : 0)));
			return correct / (double) input.size();
		}
	}

	private double entropy(Iterable<IntObjPair<Object[]>> data) {
		var hist0 = new IntIntMap1();
		for (var datum : data)
			hist0.update(datum.k, v -> (v != IntFunUtil.EMPTYVALUE ? v : 0) + 1);
		var hist1 = hist0.values();
		var sum = (double) hist1.sum();
		return hist1.toDouble(Int_Dbl.sum(c -> Math.log(c / sum)));
	}

	private int majority(Streamlet<IntObjPair<Object[]>> data) {
		return data.groupBy(IntObjPair::fst, Streamlet::size).sortByValue(Integer::compareTo).first().k;
	}

}
