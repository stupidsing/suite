package suite.algo;

import static suite.util.Friends.forInt;
import static suite.util.Friends.log;

import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.util.To;

// Introduction to Machine Learning, page 24
public class NaiveBayes {

	private int length;
	private double b;
	private float[][] ps;

	public NaiveBayes(List<Pair<int[], Boolean>> records, double threshold) {
		var nCategories = 2;
		var length_ = records.get(0).t0.length;
		var ms = new int[nCategories];
		var ws = Ints_.toArray(nCategories, cat -> 1);
		var is = To.array(nCategories, int[].class, cat -> Ints_.toArray(length_, i -> 1));

		for (var record : records) {
			var xs = record.t0;
			var cat = i(record.t1);
			ms[cat]++;
			for (var i = 0; i < length_; i++) {
				var x = xs[i];
				ws[cat] += x;
				is[cat][i] += x;
			}
		}

		length = length_;
		b = log(threshold) + log(ms[i(true)]) - log(ms[i(false)]);

		ps = To.array(nCategories, float[].class, i -> {
			var range = forInt(length_);
			var is_ = is[i];
			return range.collect(Int_Flt.lift(j -> (float) (is_[j] / ws[i]))).toArray();
		});
	}

	public boolean classify(int[] record) {
		var t = Ints_ //
				.for_(length) //
				.toDouble(Int_Dbl.sum(j -> record[j] * (log(ps[i(false)][j]) - log(ps[i(true)][j]))));
		return t <= b;
	}

	private int i(boolean b) {
		return b ? 1 : 0;
	}

}
