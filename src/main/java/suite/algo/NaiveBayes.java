package suite.algo;

import static java.lang.Math.log;
import static suite.util.Streamlet_.forInt;

import java.util.List;

import primal.Verbs.New;
import primal.adt.Pair;
import primal.primitive.IntVerbs.NewInt;
import suite.streamlet.As;

// Introduction to Machine Learning, page 24
public class NaiveBayes {

	private int length;
	private double b;
	private float[][] ps;

	public NaiveBayes(List<Pair<int[], Boolean>> records, double threshold) {
		var nCategories = 2;
		var length_ = records.get(0).k.length;
		var ms = new int[nCategories];
		var ws = NewInt.array(nCategories, cat -> 1);
		var is = New.array(nCategories, int[].class, cat -> NewInt.array(length_, i -> 1));

		for (var record : records) {
			var xs = record.k;
			var cat = i(record.v);
			ms[cat]++;
			for (var i = 0; i < length_; i++) {
				var x = xs[i];
				ws[cat] += x;
				is[cat][i] += x;
			}
		}

		length = length_;
		b = log(threshold) + log(ms[i(true)]) - log(ms[i(false)]);

		ps = New.array(nCategories, float[].class, i -> {
			var range = forInt(length_);
			var is_ = is[i];
			return range.collect(As.floats(j -> (float) (is_[j] / ws[i]))).toArray();
		});
	}

	public boolean classify(int[] record) {
		return forInt(length).toDouble(As.sum(j -> record[j] * (log(ps[i(false)][j]) - log(ps[i(true)][j])))) <= b;
	}

	private int i(boolean b) {
		return b ? 1 : 0;
	}

}
