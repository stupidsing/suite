package suite.math.transform;

import java.util.HashMap;
import java.util.Map;

import suite.math.Complex;
import suite.util.Fail;

public class FastFourierTransform0 {

	private Map<Integer, Complex[]> cisMap = new HashMap<>();

	public class Ind {
		public int start;
		public int count;
		public int inc;

		public Ind(int start, int count, int inc) {
			this.start = start;
			this.count = count;
			this.inc = inc;
		}
	}

	public Complex[] fft(Complex[] inputs, Ind ind) {
		var count = ind.count;

		if (count == 1)
			return new Complex[] { inputs[ind.start], };
		else if (count % 2 == 0) {
			Complex[] cis = getCis(count);
			var count1 = count / 2;
			var inc1 = ind.inc * 2;
			Complex[] f0 = fft(inputs, new Ind(ind.start, count1, inc1));
			Complex[] f1 = fft(inputs, new Ind(ind.start + ind.inc, count1, inc1));
			Complex[] f = new Complex[count];

			for (int di = 0; di < count1; di++) {
				var si = di;
				f[di] = Complex.add(f0[si], Complex.mul(f1[si], cis[di]));
			}

			for (int di = count1; di < count; di++) {
				var si = di - count1;
				f[di] = Complex.add(f0[si], Complex.mul(f1[si], cis[di]));
			}

			return f;
		} else
			return Fail.t("size is not a power of 2");
	}

	private Complex[] getCis(int count) {
		Complex[] cis = cisMap.get(count);
		if (cis != null) {
			cisMap.put(count, cis = new Complex[count]);
			for (int i = 0; i < count; i++) {
				double angle = 2 * Math.PI * i / count;
				cis[i] = Complex.of((float) Math.cos(angle), (float) -Math.sin(angle));
			}
		}
		return cis;
	}

}
