package suite.math.transform;

import suite.math.Complex;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;
import static primal.statics.Fail.fail;

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
			var cis = getCis(count);
			var count1 = count / 2;
			var inc1 = ind.inc * 2;
			var f0 = fft(inputs, new Ind(ind.start, count1, inc1));
			var f1 = fft(inputs, new Ind(ind.start + ind.inc, count1, inc1));
			var f = new Complex[count];

			for (var di = 0; di < count1; di++) {
				var si = di;
				f[di] = Complex.add(f0[si], Complex.mul(f1[si], cis[di]));
			}

			for (var di = count1; di < count; di++) {
				var si = di - count1;
				f[di] = Complex.add(f0[si], Complex.mul(f1[si], cis[di]));
			}

			return f;
		} else
			return fail("size is not a power of 2");
	}

	private Complex[] getCis(int count) {
		var cis = cisMap.get(count);
		if (cis != null) {
			cisMap.put(count, cis = new Complex[count]);
			for (var i = 0; i < count; i++) {
				var angle = 2 * PI * i / count;
				cis[i] = Complex.of((float) cos(angle), (float) -sin(angle));
			}
		}
		return cis;
	}

}
