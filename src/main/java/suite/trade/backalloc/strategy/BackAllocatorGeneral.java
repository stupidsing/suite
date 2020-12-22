package suite.trade.backalloc.strategy;

import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.adt.Fixie;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.IntInt_Obj;
import primal.primitive.IntVerbs.ToInt;
import primal.primitive.adt.pair.FltFltPair;
import primal.streamlet.Streamlet;
import primal.streamlet.Streamlet2;
import suite.trade.Instrument;
import suite.trade.analysis.MovingAverage;
import suite.trade.analysis.Oscillator;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSourceView;
import suite.trade.singlealloc.Strategos;
import suite.ts.BollingerBands;
import suite.ts.Quant;
import suite.ts.TimeSeries;

public class BackAllocatorGeneral {

	public static final BackAllocatorGeneral me = new BackAllocatorGeneral();

	public BackAllocator bb_ = bb(32);
	public BackAllocator cash = fixed(0f);
	public BackAllocator donHold = donchian(9).holdExtend(2).pick(5);
	public BackAllocator ema = ema(2).pick(3);
	public BackAllocator rsi = rsi(32, .7d);
	public BackAllocator pprHsi = priceProRata(Instrument.hsiSymbol);
	public BackAllocator tma = tripleExpGeometricMovingAvgs(2, 6, 18);

	public final Streamlet2<String, BackAllocator> baByName = Read //
			.<String, BackAllocator> empty2() //
			.cons("bb0", bb_) //
			.cons("bb1", bb1(32)) //
			.cons("bballoc", bbAllocate(32)) //
			.cons("bbtrend", bbTrend(32, .05d)) //
			.cons("don9", donchian(9)) //
			.cons("donalloc", donchianAllocate(9)) //
			.cons("donhold", donHold) //
			.cons("dontrend", donchianTrend(9, .05d)) //
			.cons("ema", ema) //
			.cons("half", fixed(.5d)) //
			.cons("hold", fixed(1d)) //
			.cons("lr03", lastReturn(0, 3)) //
			.cons("lr30", lastReturn(3, 0)) //
			.cons("ma1", mamr(64, 8, .15f)) //
			.cons("ma200", ma(200)) //
			.cons("mom", momentum(8)) //
			.cons("momacc", momentumAcceleration(8, 24)) //
			.cons("opcl8", openClose(8)) //
			.cons("ppr", pprHsi) //
			.cons("rsi", rsi) //
			.cons("sar", sar()) //
			.cons("trend2", trend2(.02d)) //
			.cons("turtles", turtles(20, 10, 55, 20)) //
			.cons("tma", tma) //
			.cons("varratio", varianceRatio(96)) //
			.cons("volatile", volatile_(32)) //
			.cons("xma", xma(2, 8));

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Oscillator osc = new Oscillator();
	private TimeSeries ts = new TimeSeries();

	private BackAllocatorGeneral() {
	}

	private BackAllocator bb(int tor) { // Bollingers Band
		return BackAllocator_.byPrices(prices -> {
			var sds = bb.bb(prices, tor, 0, 2f).sds;
			return Quant.fold(0, sds.length, (i, hold) -> -Quant.hold(hold, sds[i], -.5d, 0d, .5d));
		});
	}

	private BackAllocator bb1(int tor) {
		var entry = .48f;
		var exit = -.08f;

		return BackAllocator_.byPrices(prices -> {
			var sds = bb.bb(prices, tor, 0, 2f).sds;

			return Quant.enterKeep(0, sds.length, //
					i -> entry < sds[i], //
					i -> sds[i] < -entry, //
					i -> exit < sds[i], //
					i -> sds[i] < -exit);
		});
	}

	private BackAllocator bbAllocate(int tor) {
		return BackAllocator_.byPrices(prices -> {
			var sds = bb.bb(prices, tor, 0, 1f).sds;
			return index -> .5d - sds[index - 1] * .5d;
		});
	}

	private BackAllocator bbTrend(int tor, double exitThreshold) {
		return BackAllocator_.byPrices(prices -> {
			var sds = bb.bb(prices, tor, 0, 2f).sds;
			return Quant.enterUntilDrawDown(prices, exitThreshold, //
					(i, price) -> .5d <= sds[i], //
					(i, price) -> sds[i] <= -.5d);
		});
	}

	private BackAllocator donchian(int window) {
		var threshold = .05f;

		return BackAllocator_.byPrices(prices -> {
			var movingRanges = ma.movingRange(prices, window);

			return Quant.fold(0, movingRanges.length, (i, hold) -> {
				var range = movingRanges[i];
				var min = range.min;
				var max = range.max;
				var price = prices[i];
				var b = price * threshold < (max - min); // channel wide?
				return b ? Quant.hold(hold, price, min, range.median, max) : hold;
			});
		});
	}

	private BackAllocator donchianAllocate(int window) {
		return BackAllocator_.byPrices(prices -> {
			var movingRanges = ma.movingRange(prices, window);
			return index -> {
				var last = index - 1;
				var movingRange = movingRanges[last];
				var min = movingRange.min;
				var max = movingRange.max;
				return (max - prices[last]) / (max - min);
			};
		});
	}

	private BackAllocator donchianTrend(int window, double exitThreshold) {
		return BackAllocator_.byPrices(prices -> {
			var movingRanges = ma.movingRange(prices, window);
			return Quant.enterUntilDrawDown(prices, exitThreshold, //
					(i, price) -> movingRanges[i].max <= price, //
					(i, price) -> price <= movingRanges[i].min);
		});
	}

	private BackAllocator ema(int halfLife) {
		var scale = 1d / log(.8d);

		return BackAllocator_.byPrices(prices -> {
			var ema = ma.exponentialMovingAvg(prices, halfLife);

			return Quant.filterRange(1, index -> {
				var last = index - 1;
				var lastEma = ema[last];
				var latest = prices[last];
				return Quant.logReturn(lastEma, latest) * scale;
			});
		});
	}

	private BackAllocator fixed(double r) {
		return (akds, indices) -> {
			var potentialBySymbol = akds.dsByKey //
					.map((symbol, ds) -> Pair.of(symbol, r)) //
					.toList();

			return index -> potentialBySymbol;
		};
	}

	private BackAllocator lastReturn(int nWorsts, int nBests) {
		return (akds, indices) -> index -> {
			var list = akds.dsByKey //
					.mapValue(ds -> ds.lastReturn(index)) //
					.sortBy((symbol, return_) -> return_) //
					.keys() //
					.toList();

			var size = list.size();

			return Streamlet //
					.concat(Read.from(list.subList(0, nWorsts)), Read.from(list.subList(size - nBests, size))) //
					.map2(symbol -> 1d / (nWorsts + nBests)) //
					.toList();
		};
	}

	private BackAllocator ma(int tor) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs = ma.movingAvg(prices, tor);

			return index -> {
				var last = index - 1;
				return Quant.sign(movingAvgs[last], prices[last]);
			};
		});
	}

	private BackAllocator mamr(int nPastDays, int nHoldDays, float threshold) {
		var strategos = new Strategos();
		return BackAllocator_.by(strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold));
	}

	private BackAllocator momentum(int nDays) {
		return BackAllocator_.byPrices(prices -> index -> {
			var last = index - 1;
			return nDays <= last ? 30d * Quant.return_(prices[last - nDays], prices[last]) : 0d;
		});
	}

	private BackAllocator momentumAcceleration(int nDays, int nAccelDays) {
		return BackAllocator_.byPrices(prices -> index -> {
			var last1 = index - 1;
			var last0 = last1 - nAccelDays;
			if (nDays <= last0) {
				var return0 = Quant.return_(prices[last0 - nDays], prices[last0]);
				var return1 = Quant.return_(prices[last1 - nDays], prices[last1]);
				return 30d * (return1 - return0);
			} else
				return 0d;
		});
	}

	// eight-days open close
	private BackAllocator openClose(int tor) {
		return BackAllocator_.byDataSource(ds -> {
			var movingAvgOps = ma.movingAvg(ds.opens, tor);
			var movingAvgCls = ma.movingAvg(ds.closes, tor);

			return index -> {
				var last = index - 1;
				var maOp = movingAvgOps[last];
				var maCl = movingAvgCls[last];
				var diff = maCl - maOp;
				return max(maOp, maCl) * .01d < abs(diff) ? Quant.sign(diff) * 1d : 0d;
			};
		});
	}

	private BackAllocator priceProRata(String symbol) {
		var scale = 320d;

		return (akds, indices) -> {
			var prices = akds.dsByKey //
					.filter((symbol_, ds) -> Equals.string(symbol, symbol_)) //
					.uniqueResult().v.prices;

			var price0 = prices[indices[0]];

			return index -> {
				var ratio0 = Quant.return_(price0, prices[index - 1]);
				var ratio1 = scale * ratio0;
				var ratio2 = .5d + ratio1;
				return List.of(Pair.of(symbol, ratio2));
			};
		};
	}

	private BackAllocator rsi(int window, double threshold) {
		return BackAllocator_.byPrices(prices -> {
			var movement = osc.movement(prices, window);

			return Quant.filterRange(0, index -> {
				var last = index - 1;
				var dec = movement.decs[last];
				var inc = movement.incs[last];
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
			var sars = osc.sar(ds);

			return Quant.filterRange(1, index -> {
				var last = index - 1;
				return (double) Quant.sign(sars[last], ds.prices[last]);
			});
		});
	}

	private BackAllocator trend2(double threshold) {
		var daily = .1f;

		return BackAllocator_.byPrices(prices -> {
			var minMax = FltFltPair.of(Float.MAX_VALUE, Float.MIN_VALUE);

			return Quant.fold(0, prices.length, (i, hold) -> {
				var price = prices[i];
				var min = min(minMax.t0, price);
				var max = max(minMax.t1, price);
				if (threshold <= Quant.return_(min, price)) {
					hold = max(0f, hold + daily);
					max = price;
				}
				if (threshold <= Quant.return_(price, max)) {
					hold = min(0f, hold - daily);
					min = price;
				}
				minMax.update(min, max);
				return hold;
			});
		});
	}

	private BackAllocator tripleExpGeometricMovingAvgs(int d2, int d6, int d18) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs0 = ma.exponentialGeometricMovingAvg(prices, d18);
			var movingAvgs1 = ma.exponentialGeometricMovingAvg(prices, d6);
			var movingAvgs2 = ma.exponentialGeometricMovingAvg(prices, d2);

			return Quant.filterRange(1, index -> {
				var last = index - 1;
				var movingAvg0 = movingAvgs0[last];
				var movingAvg1 = movingAvgs1[last];
				var movingAvg2 = movingAvgs2[last];
				var sign0 = Quant.sign(movingAvg0, movingAvg1);
				var sign1 = Quant.sign(movingAvg1, movingAvg2);
				return sign0 == sign1 ? (double) -sign0 : 0d;
			});
		});
	}

	// http://www.metastocktools.com/downloads/turtlerules.pdf
	private BackAllocator turtles(int sys1EnterDays, int sys1ExitDays, int sys2EnterDays, int sys2ExitDays) {
		var maxUnits = 4;
		var maxUnitsTotal = 12;
		var stopN = 2;

		return (akds, indices) -> {
			var dsByKey = akds.dsByKey;
			var atrBySymbol = dsByKey.mapValue(osc::atr).toMap();

			var fixieBySymbol = dsByKey //
					.map2((symbol, ds) -> {
						var atrs = atrBySymbol.get(symbol);
						var prices = ds.prices;
						var length = prices.length;

						IntFunction<int[]> getDays = c -> ToInt.array(length, i -> {
							var price = prices[i];
							int j = i, j1;
							while (0 <= (j1 = j - 1) && Quant.sign(prices[j1], price) == c)
								j = j1;
							return i - j;
						});

						var dlos = getDays.apply(-1);
						var dhis = getDays.apply(1);

						IntInt_Obj<int[]> enterExit = (nEnterDays, nExitDays) -> {
							var holds = new int[length];
							var stopper = 0f;
							var nHold = 0;

							for (var i = 0; i < length; i++) {
								var price = prices[i];
								var dlo = dlos[i];
								var dhi = dhis[i];
								var sign = Quant.sign(nHold);

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

						var nHolds1 = enterExit.apply(sys1EnterDays, sys1ExitDays);
						var nHolds2 = enterExit.apply(sys2EnterDays, sys2ExitDays);

						Fun<int[], boolean[]> getWons = nHolds -> {
							var wasWons = new boolean[length];
							var wasWon = false;
							var isWin = false;
							var i = 0;

							while (i < length) {
								var sign = Quant.sign(nHolds[i]);
								var j = i;

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

						var wasWons1 = getWons.apply(nHolds1);
						var wasWons2 = getWons.apply(nHolds2);

						return Fixie.of(nHolds1, nHolds2, wasWons1, wasWons2);
					}) //
					.toMap();

			return index -> {
				var m0 = dsByKey //
						.keys() //
						.map2(symbol -> fixieBySymbol //
								.get(symbol) //
								.map((nHolds1, nHolds2, wasWons1) -> {
									var last = index - 1;
									return (!wasWons1[last] ? nHolds1[last] : 0) + nHolds2[last];
								})) //
						.sortByValue((nHold0, nHold1) -> Integer.compare(abs(nHold1), abs(nHold0))) //
						.toList();

				var m1 = new ArrayList<Pair<String, Integer>>();
				var sums = new int[2];

				for (var pair : m0) {
					var n = pair.v;
					var sign = 0 < n ? 0 : 1;
					var sum0 = sums[sign];
					var sum1 = max(-maxUnitsTotal, min(maxUnitsTotal, sum0 + n));
					m1.add(Pair.of(pair.k, (sums[sign] = sum1) - sum0));
				}

				return Read //
						.from2(m1) //
						.map2((symbol, nHold) -> {
							var atrs = atrBySymbol.get(symbol);
							var unit = .01d / atrs[index - 1];
							return max(-maxUnits, min(maxUnits, nHold)) * unit;
						}) //
						.toList();
			};
		};
	}

	private BackAllocator varianceRatio(int tor) {
		var vr = .95d;
		var threshold = .95d;
		var invThreshold = 1d / threshold;

		return (akds, indices) -> {
			var dsv = DataSourceView.of(0, 256, akds, (symbol, ds, period) -> ts.varianceRatio(ds.prices, tor));

			var holdsBySymbol = akds.dsByKey //
					.map2((symbol, ds) -> {
						var prices = ds.prices;
						var length = prices.length;
						var holds = new float[length];
						var hold = 0f;
						for (var index = tor; index < length; index++) {
							if (dsv.get(symbol, index) < vr) {
								var return_ = Quant.return_(prices[index - tor / 2], prices[index]);
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

	private BackAllocator volatile_(int nDays) {
		return BackAllocator_ //
				.byPrices(prices -> {
					var bandwidths = bb.bb(prices, nDays, 0, .5f).bandwidths;
					return index -> bandwidths[index - 1];
				}) //
				.pick(3);
	}

	private BackAllocator xma(int halfLife0, int halfLife1) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs0 = ma.exponentialMovingAvg(prices, halfLife0);
			var movingAvgs1 = ma.exponentialMovingAvg(prices, halfLife1);

			return Quant.filterRange(1, index -> {
				var last = index - 1;
				return -Quant.sign(movingAvgs0[last], movingAvgs1[last]);
			});
		});
	}

}
