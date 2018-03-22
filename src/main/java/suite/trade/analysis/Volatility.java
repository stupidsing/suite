package suite.trade.analysis;

import static suite.util.Friends.max;

import suite.trade.data.DataSource;
import suite.util.To;

// "High Frequency Trading: A Practical Guide to Algorithmic Strategies and Trading Systems", Irene Alridge, page 107
public class Volatility {

	private static double ln2 = Math.log(2d);
	private static double inv4ln2 = .25d / ln2;

	private int length;
	private float[] opens, closes;
	private float[] lows, highs;

	public Volatility(DataSource ds) {
		length = ds.ts.length;
		opens = ds.opens;
		closes = ds.closes;
		lows = ds.lows;
		highs = ds.highs;
	}

	// 0 < f < 1
	public float[] vol0(float f) {
		double a = 1d / (f * 2d);
		double b = 1d / ((1d - f) * 2d);
		return To.vector(length, t -> {
			double opc = opens[t] - closes[max(0, t - 1)];
			double co = closes[t] - opens[t];
			return opc * opc * a + co * co * b;
		});
	}

	public float[] vol1() {
		return To.vector(length, t -> {
			double hl = highs[t] - lows[t];
			return hl * hl * inv4ln2;
		});
	}

	// 0 < f < 1
	public float[] vol2(float f) {
		double a = .17d / f;
		double b = .83d / (1d - f) * inv4ln2;
		return To.vector(length, t -> {
			double opc = opens[t] - closes[max(0, t - 1)];
			double hl = highs[t] - lows[t];
			return opc * opc * a + hl * hl * b;
		});
	}

	public float[] vol3() {
		double a = .5d;
		double b = 1d - 2d * Math.log(2d);
		return To.vector(length, t -> {
			double hl = highs[t] - lows[t];
			double co = closes[t] - opens[t];
			return hl * hl * a + co * co * b;
		});
	}

	public float[] vol4(float f) {
		float[] vol3 = vol3();
		double a = .12d / f;
		double b = .88d / (1d - f);
		return To.vector(length, t -> {
			double opc = opens[t] - closes[max(0, t - 1)];
			return opc * opc * a + vol3[t] * b;
		});
	}

}
