package suite.trade.backalloc.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie4;
import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.TimeSeries;
import suite.primitive.IntFltPredicate;
import suite.primitive.IntInt_Obj;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.Oscillator;
import suite.trade.Oscillator.Movement;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSourceView;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.FunUtil.Fun;
import suite.util.String_;

public class BackAllocatorGeneral {

	public static final BackAllocatorGeneral me = new BackAllocatorGeneral();

	public BackAllocator bb_ = bollingerBands();
	public BackAllocator cash = cash();
	public BackAllocator donHold = donchian(9).holdExtend(2).pick(5);
	public BackAllocator ema = ema().pick(3);
	public BackAllocator rsi = rsi();
	public BackAllocator pprHsi = priceProRata(Asset.hsiSymbol);
	public BackAllocator tma = tripleExpGeometricMovingAvgs();

	public final Streamlet2<String, BackAllocator> baByName = Read //
			.<String, BackAllocator> empty2() //
			.cons("bb0", bb_) //
			.cons("bb1", bollingerBands1()) //
			.cons("bbtrend", bbTrend()) //
			.cons("don9", donchian(9)) //
			.cons("donhold", donHold) //
			.cons("dontrend", donchianTrend(9)) //
			.cons("ema", ema) //
			.cons("half", half()) //
			.cons("hold", hold()) //
			.cons("lr03", lastReturn(0, 3)) //
			.cons("lr30", lastReturn(3, 0)) //
			.cons("ma1", movingAvg()) //
			.cons("mom", momentum()) //
			.cons("momacc", momentumAcceleration()) //
			.cons("opcl8", openClose8()) //
			.cons("ppr", pprHsi) //
			.cons("rsi", rsi) //
			.cons("sar", sar()) //
			.cons("trend2", trend2()) //
			.cons("turtles", turtles(20, 10, 55, 20)) //
			.cons("tma", tma) //
			.cons("varratio", varianceRatio()) //
			.cons("xma", xma());

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Oscillator osc = new Oscillator();
	private TimeSeries ts = new TimeSeries();

	private BackAllocatorGeneral() {
	}

	private BackAllocator bollingerBands() {
		return BackAllocator_.byPrices(prices -> {
			float[] sds = bb.bb(prices, 32, 0, 2f).sds;
			return Quant.fold(0, sds.length, (i, hold) -> -Quant.hold(hold, sds[i], -.5d, 0d, .5d));
		});
	}

	private BackAllocator bollingerBands1() {
		float entry = .48f;
		float exit = -.08f;

		return BackAllocator_.byPrices(prices -> {
			float[] sds = bb.bb(prices, 32, 0, 2f).sds;

			return Quant.enterKeep(0, sds.length, //
					i -> entry < sds[i], //
					i -> sds[i] < -entry, //
					i -> exit < sds[i], //
					i -> sds[i] < -exit);
		});
	}

	private BackAllocator bbTrend() {
		float exitThreshold = .05f;

		return BackAllocator_.byPrices(prices -> {
			float[] sds = bb.bb(prices, 32, 0, 2f).sds;
			return captureEnter(prices, exitThreshold, //
					(i, price) -> .5d <= sds[i], //
					(i, price) -> sds[i] <= -.5d);
		});
	}

	private BackAllocator cash() {
		return (akds, indices) -> index -> Collections.emptyList();
	}

	private BackAllocator donchian(int window) {
		float threshold = .05f;

		return BackAllocator_.byPrices(prices -> {
			MovingRange[] movingRanges = ma.movingRange(prices, window);

			return Quant.fold(0, movingRanges.length, (i, hold) -> {
				MovingRange range = movingRanges[i];
				double min = range.min;
				double max = range.max;
				double price = prices[i];
				boolean b = price * threshold < (max - min);
				return b ? Quant.hold(hold, price, min, range.median, max) : hold;
			});
		});
	}

	private BackAllocator donchianTrend(int window) {
		float exitThreshold = .05f;

		return BackAllocator_.byPrices(prices -> {
			MovingRange[] movingRanges = ma.movingRange(prices, window);
			return captureEnter(prices, exitThreshold, //
					(i, price) -> movingRanges[i].max <= price, //
					(i, price) -> price <= movingRanges[i].min);
		});
	}

	private BackAllocator ema() {
		int halfLife = 2;
		double scale = 1d / Math.log(.8d);

		return BackAllocator_.byPrices(prices -> {
			float[] ema = ma.exponentialMovingAvg(prices, halfLife);

			return Quant.filterRange(1, index -> {
				int last = index - 1;
				double lastEma = ema[last];
				double latest = prices[last];
				return Quant.logReturn(lastEma, latest) * scale;
			});
		});
	}

	private BackAllocator half() {
		double r = .5d;
		return fixed(r);
	}

	private BackAllocator hold() {
		return fixed(1d);
	}

	private BackAllocator lastReturn(int nWorsts, int nBests) {
		return (akds, indices) -> index -> {
			List<String> list = akds.dsByKey //
					.mapValue(ds -> ds.lastReturn(index)) //
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

	private BackAllocator momentum() {
		int nDays = 8;

		return BackAllocator_.byPrices(prices -> index -> {
			int last = index - 1;
			return nDays <= last ? 30d * Quant.return_(prices[last - nDays], prices[last]) : 0d;
		});
	}

	private BackAllocator momentumAcceleration() {
		int nDays = 8;
		int nAccelDays = 24;

		return BackAllocator_.byPrices(prices -> index -> {
			int last1 = index - 1;
			int last0 = last1 - nAccelDays;
			if (nDays <= last0) {
				double return0 = Quant.return_(prices[last0 - nDays], prices[last0]);
				double return1 = Quant.return_(prices[last1 - nDays], prices[last1]);
				return 30d * (return1 - return0);
			} else
				return 0d;
		});
	}

	private BackAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return BackAllocator_.byPrices(prices -> {
			GetBuySell getBuySell = mamr.analyze(prices);

			return index -> {
				int hold = 0;
				for (int i = 0; i < index; i++)
					hold += getBuySell.get(i);
				return (double) hold;
			};
		});
	}

	// eight-days open close
	private BackAllocator openClose8() {
		return BackAllocator_.byDataSource(ds -> {
			float[] movingAvgOps = ma.movingAvg(ds.opens, 8);
			float[] movingAvgCls = ma.movingAvg(ds.closes, 8);

			return index -> {
				int last = index - 1;
				float maOp = movingAvgOps[last];
				float maCl = movingAvgCls[last];
				float diff = maCl - maOp;
				return Math.max(maOp, maCl) * .01d < Math.abs(diff) ? Quant.sign(diff) * 1d : 0d;
			};
		});
	}

	private BackAllocator priceProRata(String symbol) {
		double scale = 320d;

		return (akds, indices) -> {
			float[] prices = akds.dsByKey //
					.filter((symbol_, ds) -> String_.equals(symbol, symbol_)) //
					.uniqueResult().t1.prices;

			float price0 = prices[indices[0]];

			return index -> {
				double ratio0 = Quant.return_(price0, prices[index - 1]);
				double ratio1 = scale * ratio0;
				double ratio2 = .5d + ratio1;
				return Arrays.asList(Pair.of(symbol, ratio2));
			};
		};
	}

	private BackAllocator rsi() {
		int window = 32;
		double threshold = .7d;

		return BackAllocator_.byPrices(prices -> {
			Movement movement = osc.movement(prices, window);

			return Quant.filterRange(0, index -> {
				int last = index - 1;
				double dec = movement.decs[last];
				double inc = movement.incs[last];
				if (threshold < dec) // over-sold
					return dec - .5d;
				else if (threshold < inc) // over-bought
					return .5d - inc;
				else
					return 0d;
			});
		});
	}

	private BackAllocator sar() {
		return BackAllocator_.byDataSource(ds -> {
			float[] sars = osc.sar(ds);

			return Quant.filterRange(1, index -> {
				int last = index - 1;
				return (double) Quant.sign(sars[last], ds.prices[last]);
			});
		});
	}

	private BackAllocator trend2() {
		double threshold = .02d;
		float part = .1f;

		return BackAllocator_.byPrices(prices -> {
			float[] minMax = { Float.MAX_VALUE, Float.MIN_VALUE, };

			return Quant.fold(0, prices.length, (i, hold) -> {
				float price = prices[i];
				float min = Math.min(minMax[0], price);
				float max = Math.max(minMax[1], price);
				if (threshold <= Quant.return_(min, price)) {
					hold = Math.max(0f, hold + part);
					max = price;
				}
				if (threshold <= Quant.return_(price, max)) {
					hold = Math.min(0f, hold - part);
					min = price;
				}
				minMax[0] = min;
				minMax[1] = max;
				return hold;
			});
		});
	}

	private BackAllocator tripleExpGeometricMovingAvgs() {
		return BackAllocator_.triple(prices -> Fixie.of( //
				ma.exponentialGeometricMovingAvg(prices, 18), //
				ma.exponentialGeometricMovingAvg(prices, 6), //
				ma.exponentialGeometricMovingAvg(prices, 2)));
	}

	// http://www.metastocktools.com/downloads/turtlerules.pdf
	private BackAllocator turtles(int sys1EnterDays, int sys1ExitDays, int sys2EnterDays, int sys2ExitDays) {
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

			return index -> {
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

	private BackAllocator varianceRatio() {
		int tor = 96;
		double vr = .95d;
		double threshold = .95d;
		double invThreshold = 1d / threshold;

		return (akds, indices) -> {
			DataSourceView<String, Double> dsv = DataSourceView //
					.of(0, 256, akds, (symbol, ds, period) -> ts.varianceRatio(ds.prices, tor));

			Map<String, float[]> holdsBySymbol = akds.dsByKey //
					.map2((symbol, ds) -> {
						float[] prices = ds.prices;
						int length = prices.length;
						float[] holds = new float[length];
						float hold = 0f;
						for (int index = tor; index < length; index++) {
							if (dsv.get(symbol, index) < vr) {
								double return_ = Quant.return_(prices[index - tor / 2], prices[index]);
								hold = Quant.hold(hold, return_, threshold, 1d, invThreshold);
							} else
								hold = 0f;
							holds[index] = hold;
						}
						return holds;
					}) //
					.toMap();

			return index -> akds.dsByKey //
					.map2((symbol, ds) -> (double) holdsBySymbol.get(symbol)[index - 1]) //
					.toList();
		};
	}

	private BackAllocator xma() {
		int halfLife0 = 2;
		int halfLife1 = 8;

		return BackAllocator_.byPrices(prices -> {
			float[] movingAvgs0 = ma.exponentialMovingAvg(prices, halfLife0);
			float[] movingAvgs1 = ma.exponentialMovingAvg(prices, halfLife1);

			return Quant.filterRange(1, index -> {
				int last = index - 1;
				return movingAvgs0[last] < movingAvgs1[last] ? -1d : 1d;
			});
		});
	}

	// manual enter, auto exit when draw-down exceeded threshold
	private Int_Dbl captureEnter( //
			float[] prices, //
			float exitThreshold, //
			IntFltPredicate isEnterShort, //
			IntFltPredicate isEnterLong) {
		int length = prices.length;
		float[] holds = new float[length];
		float hold = 0f;
		float min = Float.MAX_VALUE, max = Float.MIN_VALUE;

		for (int i = 0; i < length; i++) {
			float price = prices[i];
			min = Float.min(min, price);
			max = Float.max(max, price);
			if (hold < 0f) // exit short
				hold = Quant.return_(min, price) < exitThreshold ? hold : 0f;
			else if (0f < hold) // exit long
				hold = Quant.return_(price, max) < exitThreshold ? hold : 0f;
			else if (isEnterShort.test(i, price)) {
				hold = -1f;
				min = price;
			} else if (isEnterLong.test(i, price)) {
				hold = 1f;
				max = price;
			}
			holds[i] = hold;
		}

		return index -> holds[index - 1];
	}

	private BackAllocator fixed(double r) {
		return (akds, indices) -> {
			List<Pair<String, Double>> potentialBySymbol = akds.dsByKey //
					.map((symbol, ds) -> Pair.of(symbol, r)) //
					.toList();

			return index -> potentialBySymbol;
		};
	}

}
