package suite.trade.analysis;

import suite.math.stat.Quant;
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

	public Dmi dmi(DataSource ds) {
		return dmi(ds, 14);
	}

	// https://www.tradingview.com/wiki/Directional_Movement_(DMI)
	public Dmi dmi(DataSource ds, int nDays) {
		float[] los = ds.lows;
		float[] his = ds.highs;
		int length = ds.ts.length;
		float[] dmUps = new float[length];
		float[] dmDns = new float[length];

		for (int i = 1; i < length; i++) {
			float upMove = Math.max(0, his[i] - his[i - 1]);
			float dnMove = Math.max(0, los[i - 1] - los[i]);
			dmUps[i] = dnMove < upMove ? upMove : 0f;
			dmDns[i] = upMove < dnMove ? dnMove : 0f;
		}

		float[] maDmUps = ma.movingAvg(dmUps, nDays);
		float[] maDmDns = ma.movingAvg(dmDns, nDays);
		float[] invAtrs = To.arrayOfFloats(ma.movingAvg(trueRange(ds), nDays), f -> Quant.div(1f, f));
		float[] diUps = Floats_.toArray(length, i -> maDmUps[i] * invAtrs[i]);
		float[] diDns = Floats_.toArray(length, i -> maDmDns[i] * invAtrs[i]);

		return new Dmi(Floats_.toArray(length, i -> {
			float diDn = diDns[i];
			float diUp = diUps[i];
			return Quant.div(diUp - diDn, diUp + diDn);
		}));
	}

	public class Dmi {
		public final float[] dmi;

		private Dmi(float[] dmi) {
			this.dmi = dmi;
		}

		public float[] adx(int nDays) {
			return ma.movingAvg(Floats_.of(dmi).mapFlt(Math::abs).toArray(), nDays);
		}
	}

	// commodity channel index
	public float[] cci(DataSource ds) {
		return cci(ds, 20);
	}

	public float[] cci(DataSource ds, int nDays) {
		double r = 1d / .015d;
		double i3 = 1d / 3d;
		int length = ds.ts.length;
		float[] ps = To.arrayOfFloats(length, i -> (ds.closes[i] + ds.lows[i] + ds.highs[i]) * i3);

		return To.arrayOfFloats(length, i -> {
			int i0 = Math.max(0, i - nDays + 1);
			int l = i - i0 + 1;
			double sum = 0d, sumAbsDev = 0d;
			for (int d = i0; d <= i; d++)
				sum += ps[d];
			double mean = sum / l;
			for (int d = i0; d <= i; d++)
				sumAbsDev += Math.abs(ps[d] - mean);
			double meanAbsDev = sumAbsDev / l;
			return r * (ps[i] - mean) / meanAbsDev;
		});
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
		return To.arrayOfFloats(length, i -> 1d - 1d / (1d + usMa[i] / dsMa[i]));
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
		return stochastic(ds, 5);
	}

	public float[] stochastic(DataSource ds, int kDays) {
		int dDays = 3;
		int length = ds.ts.length;
		float[] los = new float[length];
		float[] his = new float[length];

		for (int i = 0; i < length; i++) {
			float lo = Float.MAX_VALUE;
			float hi = Float.MIN_VALUE;
			for (int j = Math.max(0, i - kDays + 1); j <= i; j++) {
				lo = Math.min(lo, ds.lows[j]);
				hi = Math.max(hi, ds.highs[j]);
			}
			los[i] = lo;
			his[i] = hi;
		}

		float[] k = To.arrayOfFloats(length, i -> {
			double low = los[i];
			return (ds.closes[i] - low) / (his[i] - low);
		});

		return ma.movingAvg(k, dDays);
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
