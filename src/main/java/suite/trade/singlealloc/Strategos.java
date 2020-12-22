package suite.trade.singlealloc;

import java.util.Arrays;

import primal.primitive.FltVerbs.CopyFlt;
import primal.primitive.IntVerbs.ToInt;
import suite.math.linalg.Vector;
import suite.math.transform.DiscreteCosineTransform;
import suite.trade.analysis.MovingAverage;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.ts.Quant;
import suite.util.To;

public class Strategos {

	private MovingAverage ma = new MovingAverage();
	private Vector vec = new Vector();

	public BuySellStrategy longHold = prices -> day -> day != 0 ? 0 : 1;

	public BuySellStrategy lowPassPrediction(int windowSize, int nFutureDays, int nLowPass, float threshold) {
		var dct = new DiscreteCosineTransform();
		var nPastDays = windowSize - nFutureDays;

		return prices -> holdFixedDays(prices.length, nFutureDays, day -> {
			if (nPastDays <= day) {
				var fs0 = new float[windowSize]; // moving window
				var price0 = prices[day];

				CopyFlt.array(prices, day - nPastDays, fs0, 0, nPastDays);
				Arrays.fill(fs0, nPastDays, windowSize, price0);

				var fs1 = dct.dct(fs0);
				var fs2 = To.vector(windowSize, j -> j < nLowPass ? fs1[j] : 0f);
				var fs3 = dct.idct(fs2);

				var predict = fs3[fs3.length - 1];
				return getSignal(price0, predict, threshold);
			} else
				return 0;
		});
	}

	public BuySellStrategy macdSignalLineX(float alpha0, float alpha1, float macdAlpha) {
		return prices -> {
			var macd = ma.emacd(prices, alpha0, alpha1);
			var macdEmas = ma.exponentialMovingAvg(macd, macdAlpha);
			var diff = vec.sub(macd, macdEmas);
			return crossover(diff);
		};
	}

	// trendy; alpha0 < alpha1
	public BuySellStrategy macdZeroLineX(float alpha0, float alpha1) {
		return prices -> crossover(ma.emacd(prices, alpha0, alpha1));
	}

	public BuySellStrategy movingAvgMeanReverting(int nPastDays, int nHoldDays, float threshold) {
		return prices -> {
			var movingAvgs = ma.movingAvg(prices, nPastDays);

			return holdFixedDays(prices.length, nHoldDays, day -> {
				if (nPastDays <= day) {
					var price0 = prices[day];
					var predict = movingAvgs[day];
					return getSignal(price0, predict, threshold);
				} else
					return 0;
			});
		};
	}

	// buy/sell if ratio is positive/negative; sell/buy nHoldDays after
	private GetBuySell holdFixedDays(int nDays, int nHoldDays, GetBuySell gbs) {
		var buySells = ToInt.array(nDays, gbs::get);

		return day -> {
			var buySell0 = nHoldDays < day ? -buySells[day - nHoldDays] : 0;
			var buySell1 = buySells[day];
			return buySell0 + buySell1;
		};
	}

	// get buy/sell signal according to predicted price move direction
	private int getSignal(float price0, float price1, float threshold) {
		double ratio = Quant.return_(price0, price1);
		int signal;

		if (ratio < -threshold)
			signal = -1;
		else if (threshold < ratio)
			signal = 1;
		else
			signal = 0;

		return signal;
	}

	private GetBuySell crossover(float[] diff) {
		return day -> {
			if (0 < day) {
				var signum0 = signum(diff[day - 1]);
				var signum1 = signum(diff[day]);
				return signum0 != signum1 ? signum1 : 0;
			} else
				return 0;
		};
	}

	private int signum(float f) {
		if (f < 0)
			return -1;
		else if (0 < f)
			return 1;
		else
			return 0;
	}

}
