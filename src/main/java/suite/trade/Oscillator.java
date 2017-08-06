package suite.trade;

import suite.primitive.Floats_;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.trade.data.DataSource;
import suite.util.To;

public class Oscillator {

	private MovingAverage ma = new MovingAverage();

	// active true range
	public float[] atr(DataSource ds) {
		int n = 20;
		int length = ds.ts.length;
		float[] trs = trueRange(ds);
		float[] atrs = new float[length];
		float atr = atrs[0] = Ints_.range(n).collect(Int_Flt.lift(i -> trs[i])).sum() / n;
		double invn = 1d / n;

		for (int i = 1; i < length; i++)
			atrs[i] = atr = (float) ((atr * (n - 1) + trs[i]) * invn);

		return atrs;
	}

	// https://www.tradingview.com/wiki/Directional_Movement_(DMI)
	public float[] dmi(DataSource ds) {
		int halfLife = 7;

		int length = ds.ts.length;
		float[] dmUps = new float[length];
		float[] dmDns = new float[length];

		for (int i = 1; i < length; i++) {
			float upMove = ds.highs[i] - ds.highs[i - 1];
			float dnMove = ds.lows[i] - ds.lows[i - 1];
			dmUps[i] = Math.max(0, dnMove) < upMove ? upMove : 0f;
			dmDns[i] = Math.max(0, upMove) < dnMove ? dnMove : 0f;
		}

		float[] maDmUps = ma.exponentialMovingAvg(dmUps, halfLife);
		float[] maDmDns = ma.exponentialMovingAvg(dmDns, halfLife);
		float[] invAtrs = To.arrayOfFloats(ma.exponentialMovingAvg(trueRange(ds), halfLife), f -> 1f / f);
		float[] diUps = Floats_.toArray(length, i -> maDmUps[i] * invAtrs[i]);
		float[] diDns = Floats_.toArray(length, i -> maDmDns[i] * invAtrs[i]);

		return Floats_.toArray(length, i -> {
			float diDn = diDns[i];
			float diUp = diUps[i];
			return (diUp - diDn) / (diUp + diDn);
		});
	}

	// commodity channel index
	public float[] cci(DataSource ds) {
		int nDays = 20;
		double r = 1d / .015d;
		double i3 = 1d / 3d;
		int length = ds.ts.length;
		float[] ps = Floats_.toArray(length, i -> (float) ((ds.closes[i] + ds.lows[i] + ds.highs[i]) * i3));
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

	public Movement movement(float[] prices, int window) {
		int length = prices.length;
		byte[] cs = new byte[length];

		for (int index = 1; index < length; index++)
			cs[index] = (byte) Float.compare(prices[index - 1], prices[index]);

		float[] mvmdecs = new float[length];
		float[] mvmincs = new float[length];

		for (int index = window; index < length; index++) {
			int decs = 0, incs = 0;
			for (int i = index - window; i < index; i++) {
				int compare = cs[i];
				if (compare < 0)
					incs++;
				else if (0 < compare)
					decs++;
			}
			mvmdecs[index] = ((float) decs) / window;
			mvmincs[index] = ((float) incs) / window;
		}
		return new Movement(mvmdecs, mvmincs);

	}

	public class Movement {
		public final float[] decs;
		public final float[] incs;

		private Movement(float[] decs, float[] incs) {
			this.decs = decs;
			this.incs = incs;
		}
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

	public float[] rsi(float[] prices, int nDays) {
		int length = prices.length;
		float[] us = new float[length];
		float[] ds = new float[length];
		for (int i = 1; i < length; i++) {
			float diff = prices[i] - prices[i - 1];
			us[i] = 0f < diff ? diff : 0f;
			ds[i] = diff < 0f ? -diff : 0f;
		}
		double a = 1d / nDays;
		float[] usMa = ma.exponentialMovingAvg(us, a);
		float[] dsMa = ma.exponentialMovingAvg(ds, a);
		return Floats_.toArray(length, i -> (float) (1d - 1d / (1d + usMa[i] / dsMa[i])));
	}

	// Parabolic stop and reverse
	public float[] sar(DataSource ds) {
		float alpha = .02f;
		int length = ds.ts.length;
		float[] sars = new float[length];

		if (0 < length) {
			float ep = ds.lows[0];
			float sar;
			int i = 0;

			while (i < length) {
				sar = ep;
				ep = Float.MIN_VALUE;

				while (i < length && sar < ds.lows[i]) {
					ep = Float.max(ep, ds.highs[i]);
					sars[i++] = sar += alpha * (ep - sar);
				}

				sar = ep;
				ep = Float.MAX_VALUE;

				while (i < length && ds.highs[i] < sar) {
					ep = Float.min(ep, ds.lows[i]);
					sars[i++] = sar += alpha * (ep - sar);
				}
			}
		}

		return sars;
	}

	public float[] stochastic(DataSource ds) {
		int kDays = 5;
		int dDays = 3;
		float[] rsv = Floats_.toArray(ds.ts.length, i -> {
			double low = ds.lows[i];
			return (float) ((ds.closes[i] - low) / (ds.highs[i] - low));
		});
		float[] k = ma.movingAvg(rsv, kDays);
		float[] d = ma.movingAvg(k, dDays);
		return d;
	}

	private float[] trueRange(DataSource ds) {
		int length = ds.ts.length;
		float[] trs = new float[length];

		trs[0] = ds.highs[0] - ds.lows[0];

		for (int i = 1; i < length; i++) {
			float hi = ds.highs[i];
			float lo = ds.lows[i];
			float prevClose = ds.closes[i - 1];
			float max = Math.max(Math.abs(hi - prevClose), Math.abs(lo - prevClose));
			trs[i] = Math.max(hi - lo, max);
		}

		return trs;
	}

}
