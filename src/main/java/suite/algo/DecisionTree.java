package suite.algo;

import static suite.util.Friends.forInt;

import java.util.Random;

import suite.adt.map.IntIntMap1;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.DblIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class DecisionTree {

	private static Random random = new Random();

	public final Streamlet<IntObjPair<Object[]>> input;
	public final Obj_Int<Object[]> classifier;

	public static DecisionTree bag(Streamlet<IntObjPair<Object[]>> input, int n, int nr) { // bootstrap aggregating
		var list = input.toList();
		var size = list.size();

		var classifiers = forInt(n).map(i -> {
			var input1 = forInt(nr).map(ir -> list.get(random.nextInt(size))).collect();
			return DecisionTree.of(input1).classifier;
		});

		Obj_Int<Object[]> classifier = xs -> Read //
				.from(classifiers) //
				.map(classifier_ -> classifier_.apply(xs)) //
				.groupBy(y -> y, Streamlet::size) //
				.sortByValue(Integer::compareTo) //
				.first().t0;

		return new DecisionTree(input, classifier);
	}

	public static DecisionTree of(Streamlet<IntObjPair<Object[]>> input) {
		var default_ = majority(input);

		Obj_Int<Object[]> classifier = new Object() {
			private Obj_Int<Object[]> id3(Streamlet<IntObjPair<Object[]>> data) {
				var first = data.first();

				if (first == null)
					return xs -> default_;
				else if (data.isAll(datum -> datum.t0 == first.t0))
					return xs -> first.t0;
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

						return xs -> funs.get(xs[p]).apply(xs);
					} else
						return xs -> majority(data);
				}
			}
		}.id3(input);

		return new DecisionTree(input, classifier);
	}

	private static double entropy(Iterable<IntObjPair<Object[]>> data) {
		var hist0 = new IntIntMap1();
		for (var datum : data)
			hist0.update(datum.t0, v -> (v != IntFunUtil.EMPTYVALUE ? v : 0) + 1);
		var hist1 = hist0.values();
		var sum = (double) hist1.sum();
		return hist1.toDouble(Int_Dbl.sum(c -> Math.log(c / sum)));
	}

	private static int majority(Streamlet<IntObjPair<Object[]>> data) {
		return data.groupBy(IntObjPair::fst, Streamlet::size).sortByValue(Integer::compareTo).first().t0;
	}

	private DecisionTree(Streamlet<IntObjPair<Object[]>> input, Obj_Int<Object[]> classifier) {
		this.input = input;
		this.classifier = classifier;
	}

	public double error() {
		var correct = input.toInt(Obj_Int.sum(datum -> datum.map((y, xs) -> classifier.apply(xs) == y ? 1 : 0)));
		return correct / (double) input.size();
	}

}
