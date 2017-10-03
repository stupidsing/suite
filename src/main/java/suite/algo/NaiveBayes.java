package suite.algo;

import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.streamlet.IntStreamlet;
import suite.util.To;

// Introduction to Machine Learning, page 24
public class NaiveBayes {

	private final int length;
	private final double b;
	private final float[][] ps;

	public NaiveBayes(List<Pair<int[], Boolean>> records, double threshold) {
		int nCategories = 2;
		int length_ = records.get(0).t0.length;
		int[] ms = new int[nCategories];
		int[] ws = Ints_.toArray(nCategories, cat -> 1);
		int[][] is = To.array(nCategories, int[].class, cat -> Ints_.toArray(length_, i -> 1));

		for (Pair<int[], Boolean> record : records) {
			int[] xs = record.t0;
			int cat = i(record.t1);
			ms[cat]++;
			for (int i = 0; i < length_; i++) {
				int x = xs[i];
				ws[cat] += x;
				is[cat][i] += x;
			}
		}

		length = length_;
		b = Math.log(threshold) + Math.log(ms[i(true)]) - Math.log(ms[i(false)]);

		ps = To.array(nCategories, float[].class, i -> {
			IntStreamlet range = Ints_.range(length_);
			int[] is_ = is[i];
			return range.collect(Int_Flt.lift(j -> (float) (is_[j] / ws[i]))).toArray();
		});
	}

	public boolean classify(int[] record) {
		double t = Ints_ //
				.range(length) //
				.collectAsDouble(Int_Dbl.sum(j -> record[j] * (Math.log(ps[i(false)][j]) - Math.log(ps[i(true)][j]))));
		return t <= b;
	}

	private int i(boolean b) {
		return b ? 1 : 0;
	}

}
