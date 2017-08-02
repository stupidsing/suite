package suite.trade.backalloc.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Fixie_.Fixie4;
import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntInt_Obj;
import suite.primitive.Ints_;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.Oscillator;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.FunUtil.Fun;
import suite.util.String_;

public class BackAllocator_ {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static Oscillator osc = new Oscillator();
	private static Statistic stat = new Statistic();
	private static TimeSeries ts = new TimeSeries();

	public static BackAllocator bbSlope() {
		return BackAllocator.byPrices(prices -> {
			float[] percentbs = bb.bb(prices, 32, 0, 2f).percentbs;
			float[] ma_ = ma.movingAvg(percentbs, 6);
			float[] diffs = ts.differences(3, ma_);

			return index -> {
				int last = index - 1;
				float percentb = ma_[last];
				float diff = diffs[last];
				if (percentb < .2d && .015d < diff)
					return 1d;
				else if (-.8d < percentb && diff < -.015d)
					return -1d;
				else
					return 0d;
			};
		});
	}

	public static BackAllocator bollingerBands() {
		return bollingerBands_(32, 0, 2f);
	}

	public static BackAllocator cash() {
		return (akds, indices) -> index -> Collections.emptyList();
	}

	public static BackAllocator donchian(int window) {
		float threshold = .05f;

		return BackAllocator.byPrices(prices -> {
			MovingRange[] movingRanges = ma.movingRange(prices, window);
			int length = movingRanges.length;
			float[] holds = new float[length];
			float hold = 0f;

			for (int i = 0; i < length; i++) {
				MovingRange range = movingRanges[i];
				double min = range.min;
				double max = range.max;
				double price = prices[i];
				double vol = (max - min) / (price * threshold);
				if (1d < vol)
					hold = hold(hold, price, min, range.median, max);
				holds[i] = hold;
			}

			return index -> (double) holds[index - 1];
		});
	}

	public static BackAllocator ema() {
		int halfLife = 2;
		double scale = 1d / Math.log(.8d);

		return BackAllocator.byPrices(prices -> {
			float[] ema = ma.exponentialMovingAvg(prices, halfLife);

			return index -> {
				int last = index - 1;
				double lastEma = ema[last];
				double latest = prices[last];
				return Quant.logReturn(lastEma, latest) * scale;
			};
		});
	}

	// reverse draw-down; trendy strategy
	public static BackAllocator revDrawdown() {
		return BackAllocator.byPrices(prices -> index -> {
			int i = index - 1;
			int i0 = Math.max(0, i - 128);
			int ix = i;
			int dir = 0;

			float lastPrice = prices[ix];
			float priceo = lastPrice;
			int io = i;

			for (; i0 <= i; i--) {
				float price = prices[i];
				int dir1 = Quant.sign(price, lastPrice);

				if (dir != 0 && dir != dir1) {
					double r = (index - io) / (double) (index - i);
					return .36d < r ? Quant.return_(priceo, lastPrice) * r * 4d : 0d;
				} else
					dir = dir1;

				if (Quant.sign(price, priceo) == dir) {
					priceo = price;
					io = i;
				}
			}

			return 0d;
		});
	}

	public static BackAllocator lastReturn(int nWorsts, int nBests) {
		return (akds, indices) -> index -> {
			List<String> list = akds.dsByKey //
					.map2((symbol, ds) -> ds.lastReturn(index)) //
					.sortBy((symbol, return_) -> return_) //
					.keys() //
					.toList();

			int size = list.size();

			return Streamlet //
					.concat(Read.from(list.subList(0, nWorsts)), Read.from(list.subList(size - nBests, size))) //
					.map2(symbol -> 1d / (nWorsts + nBests)) //
					.toList();
		};
	}

	public static BackAllocator lastReturnsProRata() {
		return (akds, indices) -> index -> {
			Streamlet2<String, Double> returns = akds.dsByKey //
					.map2((symbol, ds) -> ds.lastReturn(index)) //
					.filterValue(return_ -> 0d < return_) //
					.collect(As::streamlet2);

			double sum = returns.collectAsDouble(ObjObj_Dbl.sum((symbol, price) -> price));
			return returns.mapValue(return_ -> return_ / sum).toList();
		};
	}

	public static BackAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return BackAllocator.byPrices(prices -> {
			GetBuySell getBuySell = mamr.analyze(prices);

			return index -> {
				int hold = 0;
				for (int i = 0; i < index; i++)
					hold += getBuySell.get(i);
				return (double) hold;
			};
		});
	}

	public static BackAllocator movingAvgMedian() {
		int windowSize0 = 4;
		int windowSize1 = 12;

		return BackAllocator.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, windowSize0);
			float[] movingAvgs1 = ma.movingAvg(prices, windowSize1);
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				int last = index - 1;
				float movingAvg0 = movingAvgs0[last];
				float movingAvg1 = movingAvgs1[last];
				float movingMedian0 = movingRanges0[last].median;
				float movingMedian1 = movingRanges1[last].median;
				int sign0 = Quant.sign(movingAvg0, movingMedian0);
				int sign1 = Quant.sign(movingAvg1, movingMedian1);
				return sign0 == sign1 ? (double) sign0 : 0d;
			};
		});
	}

	public static BackAllocator movingMedianMeanRevn() {
		int windowSize0 = 1;
		int windowSize1 = 32;

		return BackAllocator.byPrices(prices -> {
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				int last = index - 1;
				return Quant.return_(movingRanges0[last].median, movingRanges1[last].median);
			};
		});
	}

	public static BackAllocator ofSingle(String symbol) {
		return (akds, indices) -> index -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static BackAllocator pairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocator_ //
				.rsi_(32, .3d, .7d) //
				.relativeToIndex(cfg, symbol0) //
				.filterByAsset(symbol -> String_.equals(symbol, symbol1));
	}

	public static BackAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		BackAllocator ba0 = (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();
			DataSource ds0 = dsBySymbol_.get(symbol0);
			DataSource ds1 = dsBySymbol_.get(symbol1);

			return index -> {
				int ix = index - 1;
				int i0 = ix - tor;
				double p0 = ds0.get(i0).t1, px = ds0.get(ix).t1;
				double q0 = ds1.get(i0).t1, qx = ds1.get(ix).t1;
				double pdiff = Quant.return_(p0, px);
				double qdiff = Quant.return_(q0, qx);

				if (threshold < Math.abs(pdiff - qdiff))
					return Arrays.asList( //
							Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d), //
							Pair.of(pdiff < qdiff ? symbol1 : symbol0, -1d));
				else
					return Collections.emptyList();
			};
		};

		return ba0.filterByAsset(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	public static BackAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static BackAllocator sar() {
		return BackAllocator.bySymbol(ds -> {
			float[] sars = osc.sar(ds);

			return index -> {
				int last = index - 1;
				return (double) Quant.sign(sars[last], ds.prices[last]);
			};
		});
	}

	public static BackAllocator sum(BackAllocator... bas) {
		return (akds, indices) -> {
			Streamlet<OnDateTime> odts = Read.from(bas) //
					.map(ba -> ba.allocate(akds, indices)) //
					.collect(As::streamlet);

			return index -> odts //
					.flatMap(odt -> odt.onDateTime(index)) //
					.groupBy(Pair::first_, st -> st.collectAsDouble(Obj_Dbl.sum(Pair::second))) //
					.toList();
		};
	}

	public static BackAllocator tripleExpGeometricMovingAvgs() {
		return tripleMovingAvgs(prices -> Fixie.of( //
				ma.exponentialGeometricMovingAvg(prices, 18), //
				ma.exponentialGeometricMovingAvg(prices, 6), //
				ma.exponentialGeometricMovingAvg(prices, 2)));
	}

	public static BackAllocator tripleMovingAvgs() {
		return tripleMovingAvgs(prices -> Fixie.of( //
				ma.movingAvg(prices, 52), //
				ma.movingAvg(prices, 26), //
				ma.movingAvg(prices, 9)));
	}

	private static BackAllocator tripleMovingAvgs(Fun<float[], Fixie3<float[], float[], float[]>> fun) {
		return BackAllocator.byPrices(prices -> {
			Fixie3<float[], float[], float[]> fixie = fun.apply(prices);

			return index -> fixie //
					.map((movingAvgs0, movingAvgs1, movingAvgs2) -> {
						int last = index - 1;
						int sign0 = Quant.sign(movingAvgs0[last], movingAvgs1[last]);
						int sign1 = Quant.sign(movingAvgs1[last], movingAvgs2[last]);
						return sign0 == sign1 ? (double) -sign0 : 0d;
					});
		});
	}

	// http://www.metastocktools.com/downloads/turtlerules.pdf
	public static BackAllocator turtles(int sys1EnterDays, int sys1ExitDays, int sys2EnterDays, int sys2ExitDays) {
		int maxUnits = 4;
		int maxUnitsTotal = 12;
		int stopN = 2;

		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsByKey = akds.dsByKey;
			Map<String, float[]> atrBySymbol = dsByKey.mapValue(osc::atr).toMap();

			Map<String, Fixie4<int[], int[], boolean[], boolean[]>> fixieBySymbol = dsByKey //
					.map2((symbol, ds) -> {
						float[] atrs = atrBySymbol.get(symbol);
						float[] prices = ds.prices;
						int length = prices.length;

						IntFunction<int[]> getDays = c -> Ints_.toArray(length, i -> {
							float price = prices[i];
							int j = i, j1;
							while (0 <= (j1 = j - 1) && Quant.sign(prices[j1], price) == c)
								j = j1;
							return i - j;
						});

						int[] dlos = getDays.apply(-1);
						int[] dhis = getDays.apply(1);

						IntInt_Obj<int[]> enterExit = (nEnterDays, nExitDays) -> {
							int[] holds = new int[length];
							float stopper = 0f;
							int nHold = 0;

							for (int i = 0; i < length; i++) {
								float price = prices[i];
								int dlo = dlos[i];
								int dhi = dhis[i];
								int sign = Quant.sign(nHold);

								if (sign == Quant.sign(price, stopper) // stops
										|| sign == 1 && nExitDays <= dlo // exit
										|| sign == -1 && nExitDays <= dhi) // exit
									nHold = 0;

								if (nEnterDays <= dlo) { // short entry
									nHold--;
									stopper = price + stopN * atrs[i];
								}

								if (nEnterDays <= dhi) { // long entry
									nHold++;
									stopper = price - stopN * atrs[i];
								}

								holds[i] = nHold;
							}

							return holds;
						};

						int[] nHolds1 = enterExit.apply(sys1EnterDays, sys1ExitDays);
						int[] nHolds2 = enterExit.apply(sys2EnterDays, sys2ExitDays);

						Fun<int[], boolean[]> getWons = nHolds -> {
							boolean[] wasWons = new boolean[length];
							boolean wasWon = false;
							boolean isWin = false;
							int i = 0;

							while (i < length) {
								int sign = Quant.sign(nHolds[i]);
								int j = i;

								while (j < length && sign == Quant.sign(nHolds[j]))
									j++;

								if (sign != 0) {
									wasWon = isWin;
									isWin = j < length && sign == Quant.sign(prices[i], prices[j]);
								}

								while (i < j)
									wasWons[i++] = wasWon;
							}

							return wasWons;
						};

						boolean[] wasWons1 = getWons.apply(nHolds1);
						boolean[] wasWons2 = getWons.apply(nHolds2);

						return Fixie.of(nHolds1, nHolds2, wasWons1, wasWons2);
					}) //
					.toMap();

			return index ->

			{
				List<Pair<String, Integer>> m0 = dsByKey //
						.keys() //
						.map2(symbol -> fixieBySymbol //
								.get(symbol) //
								.map((nHolds1, nHolds2, wasWons1) -> {
									int last = index - 1;
									return (!wasWons1[last] ? nHolds1[last] : 0) + nHolds2[last];
								})) //
						.sortByValue((nHold0, nHold1) -> Integer.compare(Math.abs(nHold1), Math.abs(nHold0))) //
						.toList();

				List<Pair<String, Integer>> m1 = new ArrayList<>();
				int[] sums = new int[2];

				for (Pair<String, Integer> pair : m0) {
					int n = pair.t1;
					int sign = 0 < n ? 0 : 1;
					int sum0 = sums[sign];
					int sum1 = Math.max(-maxUnitsTotal, Math.min(maxUnitsTotal, sum0 + n));
					m1.add(Pair.of(pair.t0, (sums[sign] = sum1) - sum0));
				}

				return Read //
						.from2(m1) //
						.map2((symbol, nHold) -> {
							float[] atrs = atrBySymbol.get(symbol);
							int last = index - 1;
							double unit = .01d / atrs[last];
							return Math.max(-maxUnits, Math.min(maxUnits, nHold)) * unit;
						}) //
						.toList();
			};
		};
	}

	public static BackAllocator variableBollingerBands() {
		return BackAllocator.byPrices(prices -> index -> {
			int last = index - 1;
			double hold = 0d;

			for (int window = 1; hold == 0d && window < 256; window++) {
				float price = prices[last];
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(prices, last - window, last));
				double mean = mv.mean;
				double diff = 3d * mv.standardDeviation();

				if (price < mean - diff)
					hold = 1d;
				else if (mean + diff < price)
					hold = -1d;
			}

			return hold;
		});
	}

	public static BackAllocator xma() {
		int halfLife0 = 2;
		int halfLife1 = 8;

		return BackAllocator.byPrices(prices -> {
			float[] movingAvgs0 = ma.exponentialMovingAvg(prices, halfLife0);
			float[] movingAvgs1 = ma.exponentialMovingAvg(prices, halfLife1);

			return index -> {
				int last = index - 1;
				return movingAvgs0[last] < movingAvgs1[last] ? -1d : 1d;
			};
		});
	}

	private static BackAllocator bollingerBands_(int backPos0, int backPos1, float k) {
		return BackAllocator.byPrices(prices -> {
			float[] percentbs = bb.bb(prices, backPos0, backPos1, k).percentbs;
			int length = percentbs.length;
			float[] holds = new float[length];
			float hold = 0f;

			for (int i = 0; i < length; i++)
				holds[i] = (hold = hold(hold, percentbs[i], 0f, .5f, 1f));

			return index -> (double) holds[index - 1];
		});
	}

	private static BackAllocator rsi_(int window, double threshold0, double threshold1) {
		return BackAllocator.byPrices(prices -> index -> {
			int gt = 0, ge = 0;
			for (int i = index - window; i < index; i++) {
				int compare = Float.compare(prices[i - 1], prices[i]);
				gt += compare < 0 ? 1 : 0;
				ge += compare <= 0 ? 1 : 0;
			}
			double rsigt = (double) gt / window;
			double rsige = (double) ge / window;
			if (rsige < threshold0) // over-sold
				return .5d - rsige;
			else if (threshold1 < rsigt) // over-bought
				return .5d - rsigt;
			else
				return 0d;
		});
	}

	private static float hold(float hold, double ind, double th0, double th1, double th2) {
		if (ind <= th0)
			hold = 1f;
		else if (ind < th1)
			hold = Math.max(0f, hold);
		else if (ind < th2)
			hold = Math.min(0f, hold);
		else
			hold = -1f;
		return hold;
	}

}
