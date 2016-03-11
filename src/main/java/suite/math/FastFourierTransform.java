package suite.math;

import java.util.HashMap;
import java.util.Map;

public class FastFourierTransform {

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

	public Complex[] fft(Complex inputs[], Ind ind) {
		int count = ind.count;

		if (count == 1)
			return new Complex[] { inputs[ind.start], };
		else if (count % 2 == 0) {
			Complex cis[] = getCis(count);
			int count1 = count / 2;
			int inc1 = ind.inc * 2;
			Complex f0[] = fft(inputs, new Ind(ind.start, count1, inc1));
			Complex f1[] = fft(inputs, new Ind(ind.start + ind.inc, count1, inc1));
			Complex f[] = new Complex[count];

			for (int i = 0; i < count1; i++) {
				f[i] = Complex.add(f[i], f0[i]);
				f[i] = Complex.add(f[i], Complex.mul(f1[i], cis[i]));
			}

			for (int i = count1; i < count; i++) {
				f[i] = Complex.add(f[i], f0[i - count1]);
				f[i] = Complex.add(f[i], Complex.mul(f1[i - count1], cis[i]));
			}

			return f;
		} else
			throw new RuntimeException("Size is not a power of 2");
	}

	private Complex[] getCis(int count) {
		Complex[] cis = cisMap.get(count);
		if (cis != null) {
			cisMap.put(count, cis = new Complex[count]);
			for (int i = 0; i < count; i++) {
				double angle = 2 * Math.PI * i / count;
				cis[i] = new Complex((float) Math.cos(angle), (float) -Math.sin(angle));
			}
		}
		return cis;
	}

}
