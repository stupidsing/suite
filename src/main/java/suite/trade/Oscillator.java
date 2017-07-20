package suite.trade;

import suite.trade.data.DataSource;
import suite.util.To;

public class Oscillator {

	private MovingAverage ma = new MovingAverage();

	// commodity channel index
	public float[] cci(DataSource ds) {
		int nDays = 20;
		double r = 1d / .015d;
		double i3 = 1d / 3d;
		int length = ds.ts.length;
		float[] ps = To.arrayOfFloats(length, i -> (float) ((ds.closes[i] + ds.lows[i] + ds.highs[i]) * i3));
		float[] ccis = new float[length];
		for (int i = 0; i < length; i++) {
			int i0 = Math.max(0, i - nDays);
			double sum = 0d, sumAbsDev = 0d;
			for (int d = i0; d < nDays; d++)
				sum += ps[i - d];
			double mean = sum / (i - i0);
			for (int d = i0; d < nDays; d++)
				sumAbsDev += Math.abs(ps[i - d] - mean);
			double meanAbsDev = sumAbsDev / (i - i0);
			ccis[i] = (float) (r * (ps[i] - mean) / meanAbsDev);
		}
		return ccis;
	}

	// on-balance volume
	public float[] obv(DataSource ds) {
		int length = ds.ts.length;
		float[] obvs = new float[length];
		double obv = 0d;
		for (int i = 1; i < length; i++) {
			int c = Float.compare(ds.closes[i - 1], ds.closes[i]);
			float volume = ds.volumes[i];
			if (c < 0)
				obv += volume;
			else if (0 < c)
				obv -= volume;
			obvs[i] = (float) obv;
		}
		return obvs;
	}

	public float[] sar(DataSource ds) {
		float alpha = .02f;
		int length = ds.ts.length;
		float[] sars = new float[length];

		if (0 < length) {
			float min = Float.MAX_VALUE;
			float max = Float.MIN_VALUE;
			float sar = ds.prices[0];
			int i = 0;

			while (i < length) {
				while (i < length) {
					max = Float.max(max, ds.highs[i]);
					if (sar < ds.lows[i])
						sar += alpha * (max - sar);
					else {
						min = Float.MAX_VALUE;
						sar = max;
						break;
					}
					sars[i++] = sar;
				}

				while (i < length) {
					min = Float.min(min, ds.lows[i]);
					if (ds.highs[i] < sar)
						sar += alpha * (min - sar);
					else {
						max = Float.MIN_VALUE;
						sar = min;
						break;
					}
					sars[i++] = sar;
				}
			}
		}

		return sars;
	}

	public float[] stochastic(DataSource ds) {
		int kDays = 5;
		int dDays = 3;
		float[] rsv = To.arrayOfFloats(ds.ts.length, i -> {
			double low = ds.lows[i];
			return (float) ((ds.closes[i] - low) / (ds.highs[i] - low));
		});
		float[] k = ma.movingAvg(rsv, kDays);
		float[] d = ma.movingAvg(k, dDays);
		return d;
	}

}
