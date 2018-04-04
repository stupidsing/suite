package suite.trade.analysis;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import suite.primitive.Floats_;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.trade.data.DataSource;
import suite.util.To;
import ts.Quant;

public class Oscillator {

	private MovingAverage ma = new MovingAverage();

	// active true range
	public float[] atr(DataSource ds) {
		var n = 20;
		var length = ds.ts.length;
		var trs = trueRange(ds);
		var atrs = new float[length];
		var atr = atrs[0] = Ints_.range(n).collect(Int_Flt.lift(i -> trs[i])).sum() / n;
		var invn = 1d / n;

		for (int i = 1; i < length; i++)
			atrs[i] = atr = (float) ((atr * (n - 1) + trs[i]) * invn);

		return atrs;
	}

	public Dmi dmi(DataSource ds) {
		return dmi(ds, 14);
	}

	// https://www.tradingview.com/wiki/Directional_Movement_(DMI)
	public Dmi dmi(DataSource ds, int nDays) {
		var los = ds.lows;
		var his = ds.highs;
		var length = ds.ts.length;
		var dmUps = new float[length];
		var dmDns = new float[length];

		for (int i = 1; i < length; i++) {
			float upMove = max(0, his[i] - his[i - 1]);
			float dnMove = max(0, los[i - 1] - los[i]);
			dmUps[i] = dnMove < upMove ? upMove : 0f;
			dmDns[i] = upMove < dnMove ? dnMove : 0f;
		}

		float[] maDmUps = ma.movingAvg(dmUps, nDays);
		float[] maDmDns = ma.movingAvg(dmDns, nDays);
		float[] invAtrs = To.vector(ma.movingAvg(trueRange(ds), nDays), f -> Quant.div(1f, f));
		float[] diUps = Floats_.toArray(length, i -> maDmUps[i] * invAtrs[i]);
		float[] diDns = Floats_.toArray(length, i -> maDmDns[i] * invAtrs[i]);

		return new Dmi(Floats_.toArray(length, i -> {
			var diDn = diDns[i];
			var diUp = diUps[i];
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
		var r = 1d / .015d;
		var i3 = 1d / 3d;
		var length = ds.ts.length;
		float[] ps = To.vector(length, i -> (ds.closes[i] + ds.lows[i] + ds.highs[i]) * i3);

		return To.vector(length, i -> {
			int i0 = max(0, i - nDays + 1);
			var l = i - i0 + 1;
			double sum = 0d, sumAbsDev = 0d;
			for (int d = i0; d <= i; d++)
				sum += ps[d];
			var mean = sum / l;
			for (int d = i0; d <= i; d++)
				sumAbsDev += Math.abs(ps[d] - mean);
			var meanAbsDev = sumAbsDev / l;
			return r * (ps[i] - mean) / meanAbsDev;
		});
	}

	public Movement movement(float[] prices, int window) {
		var length = prices.length;
		byte[] cs = new byte[length];

		for (int index = 1; index < length; index++)
			cs[index] = (byte) Float.compare(prices[index - 1], prices[index]);

		var mvmdecs = new float[length];
		var mvmincs = new float[length];

		for (int index = window; index < length; index++) {
			int decs = 0, incs = 0;
			for (int i = index - window; i < index; i++) {
				var compare = cs[i];
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
		var length = ds.ts.length;
		var obvs = new float[length];
		var obv = 0d;
		for (int i = 1; i < length; i++) {
			int c = Float.compare(ds.closes[i - 1], ds.closes[i]);
			var volume = ds.volumes[i];
			if (c < 0)
				obv += volume;
			else if (0 < c)
				obv -= volume;
			obvs[i] = (float) obv;
		}
		return obvs;
	}

	public float[] rsi(float[] prices, int nDays) {
		var length = prices.length;
		var us = new float[length];
		var ds = new float[length];
		for (int i = 1; i < length; i++) {
			var diff = prices[i] - prices[i - 1];
			us[i] = 0f < diff ? diff : 0f;
			ds[i] = diff < 0f ? -diff : 0f;
		}
		var a = 1d / nDays;
		float[] usMa = ma.exponentialMovingAvg(us, a);
		float[] dsMa = ma.exponentialMovingAvg(ds, a);
		return To.vector(length, i -> 1d - 1d / (1d + usMa[i] / dsMa[i]));
	}

	// Parabolic stop and reverse
	public float[] sar(DataSource ds) {
		var alpha = .02f;
		var length = ds.ts.length;
		var sars = new float[length];

		if (0 < length) {
			var ep = ds.lows[0];
			float sar;
			var i = 0;

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
		var dDays = 3;
		var length = ds.ts.length;
		var los = new float[length];
		var his = new float[length];

		for (int i = 0; i < length; i++) {
			var lo = Float.MAX_VALUE;
			var hi = Float.MIN_VALUE;
			for (int j = max(0, i - kDays + 1); j <= i; j++) {
				lo = min(lo, ds.lows[j]);
				hi = max(hi, ds.highs[j]);
			}
			los[i] = lo;
			his[i] = hi;
		}

		float[] k = To.vector(length, i -> {
			var low = los[i];
			return (ds.closes[i] - low) / (his[i] - low);
		});

		return ma.movingAvg(k, dDays);
	}

	private float[] trueRange(DataSource ds) {
		var length = ds.ts.length;
		var trs = new float[length];

		trs[0] = ds.highs[0] - ds.lows[0];

		for (int i = 1; i < length; i++) {
			var hi = ds.highs[i];
			var lo = ds.lows[i];
			var prevClose = ds.closes[i - 1];
			float max = max(Math.abs(hi - prevClose), Math.abs(lo - prevClose));
			trs[i] = max(hi - lo, max);
		}

		return trs;
	}

}
