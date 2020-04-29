package suite.trade.backalloc;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import primal.MoreVerbs.Read;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Union;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.DblDbl_Dbl;
import primal.primitive.adt.pair.DblFltPair;
import primal.primitive.fp.AsDbl;
import primal.streamlet.Streamlet;
import suite.math.numeric.Statistic;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.Usex;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Datum;
import suite.trade.data.TradeCfg;
import suite.trade.walkforwardalloc.WalkForwardAllocator;
import suite.ts.Quant;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface BackAllocator {

	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential values, or
		 *         null if the strategy do not want to trade on that date. The assets
		 *         will be allocated according to potential values pro-rata.
		 */
		public List<Pair<String, Double>> onDateTime(int index);
	}

	public default BackAllocator byRiskOfReturn() {
		var stat = new Statistic();
		var nDays = 32;

		return (akds, indices) -> {
			var returnsByKey = akds.dsByKey.mapValue(DataSource::returns).toMap();
			var ba0 = allocate(akds, indices);

			return index -> Read
					.from2(ba0.onDateTime(index))
					.map2((symbol, potential) -> {
						var returns = Arrays.copyOfRange(returnsByKey.get(symbol), index - nDays, index);
						return potential / stat.variance(returns);
					})
					.toList();
		};
	}

	public default BackAllocator byTime(IntPredicate monthPred) {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> monthPred.test(Time.ofEpochSec(akds.ts[index - 1]).month())
					? onDateTime.onDateTime(index)
					: List.of();
		};
	}

	public default BackAllocConfiguration cfg(Fun<Time, Streamlet<Instrument>> instrumentsFun) {
		return new BackAllocConfiguration(instrumentsFun, this);
	}

	public default BackAllocConfiguration cfgUnl(Fun<Time, Streamlet<Instrument>> instrumentsFun) {
		return pick(40).unleverage().cfg(instrumentsFun);
	}

	public default BackAllocator dump() {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> {
				var ratioBySymbol = onDateTime.onDateTime(index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public default BackAllocator even() {
		var ba1 = longOnly();

		return (akds, indices) -> {
			var onDateTime = ba1.allocate(akds, indices);

			return index -> {
				var potentialBySymbol = Read
						.from2(onDateTime.onDateTime(index))
						.collect();

				var size = potentialBySymbol.size();

				if (0 < size) {
					var each = 1d / size;

					return potentialBySymbol
							.filterKey(symbol -> !Equals.string(symbol, Instrument.cashSymbol))
							.mapValue(potential -> 1d / each)
							.toList();
				} else
					return List.of();
			};
		};
	}

	public default BackAllocator filterBySymbol(Predicate<String> pred) {
		return (akds0, indices) -> {
			var akds1 = new AlignKeyDataSource<>(akds0.ts, akds0.dsByKey.filterKey(pred));

			return allocate(akds1, indices)::onDateTime;
		};
	}

	public default BackAllocator filterByIndex(TradeCfg cfg) {
		return filterByIndexReturn(cfg, Usex.sp500);
	}

	public default BackAllocator filterByIndexReturn(TradeCfg cfg, String indexSymbol) {
		var indexDataSource = cfg.dataSource(indexSymbol);

		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> {
				var date = Time.ofEpochSec(akds.ts[index - 1]).date();
				var t0 = date.addDays(-7).epochSec();
				var tx = date.epochSec();
				var ids = indexDataSource.range(t0, tx);

				var indexPrice0 = ids.get(-1).t1;
				var indexPricex = ids.get(-2).t1;
				var indexReturn = Quant.return_(indexPrice0, indexPricex);

				return -.03f < indexReturn
						? onDateTime.onDateTime(index)
						: List.of();
			};
		};
	}

	public default BackAllocator frequency(int freq) {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return new OnDateTime() {
				private Time time0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDateTime(int index) {
					var time_ = Time.ofEpochSec(akds.ts[index - 1]);
					var time1 = time_.addDays(-(time_.epochDay() % freq));

					if (!Equals.ab(time0, time1)) {
						time0 = time1;
						result0 = onDateTime.onDateTime(index);
					}

					return result0;
				}
			};
		};
	}

	public default BackAllocator holdDelay(int period) {
		return hold(period, Math::min);
	}

	public default BackAllocator holdExtend(int period) {
		return hold(period, Math::max);
	}

	public default BackAllocator hold(int period, DblDbl_Dbl fun) {
		return (akds, indices) -> {
			var queue = new ArrayDeque<Map<String, Double>>();
			var onDateTime = allocate(akds, indices);

			return index -> {
				queue.addLast(Read
						.from2(onDateTime.onDateTime(index))
						.toMap());

				while (period < queue.size())
					queue.removeFirst();

				var map = new HashMap<String, Double>();

				for (var m : queue)
					for (var e : m.entrySet())
						map.compute(e.getKey(), (k, v) -> fun.apply(v != null ? v : 0d, e.getValue()));

				return Read.from2(map).toList();
			};
		};
	}

	public default BackAllocator january() {
		return byTime(month -> month == 1);
	}

	public default BackAllocator longOnly() {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> Read
					.from2(onDateTime.onDateTime(index))
					.map2((symbol, potential) -> {
						return Double.isFinite(potential) ? potential : fail("potential is " + potential);
					})
					.filterValue(potential -> 0d < potential)
					.toList();
		};
	}

	public default BackAllocator pick(int top) {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> Read
					.from2(onDateTime.onDateTime(index))
					.sortByValue((r0, r1) -> Compare.objects(r1, r0))
					.take(top)
					.toList();
		};
	}

	public default BackAllocator reallocate() {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);

			return index -> {
				var potentialBySymbol = onDateTime.onDateTime(index);
				return BackAllocatorUtil.scale(potentialBySymbol, 1d / BackAllocatorUtil.totalPotential(potentialBySymbol));
			};
		};
	}

	public default BackAllocator relative(DataSource indexDataSource) {
		return (akds0, times_) -> {
			var dsBySymbol1 = akds0.dsByKey
					.mapValue(ds0 -> {
						var indexPrices = indexDataSource.alignBeforePrices(ds0.ts).prices;
						var length = ds0.ts.length;
						var data1 = new Datum[length];

						for (var i = 0; i < length; i++) {
							var r = 1d / indexPrices[i];
							var t = ds0.ts[i];
							data1[i] = new Datum(
									t,
									t + DataSource.tickDuration,
									(float) (ds0.opens[i] * r),
									(float) (ds0.closes[i] * r),
									(float) (ds0.lows[i] * r),
									(float) (ds0.highs[i] * r),
									ds0.volumes[i]);
						}

						return DataSource.of(Read.from(data1));
					})
					.collect();

			return allocate(new AlignKeyDataSource<>(akds0.ts, dsBySymbol1), times_)::onDateTime;
		};
	}

	public default BackAllocator relativeToHsi(TradeCfg cfg) {
		return relativeToIndex(cfg, "^HSI");
	}

	public default BackAllocator relativeToIndex(TradeCfg cfg, String indexSymbol) {
		return relative(cfg.dataSource(indexSymbol));
	}

	public default BackAllocator sellInMay() {
		return byTime(month -> month < 5 || 11 <= month);
	}

	public default BackAllocator stopLoss(double percent) {
		return stop(percent, 1E6d);
	}

	public default BackAllocator stop(double stopLoss, double stopGain) {
		return (akds, indices) -> {
			var onDateTime = allocate(akds, indices);
			var dsBySymbol = akds.dsByKey.toMap();
			var mutable = Mutable.<Map<String, Double>> of(new HashMap<>());
			var entriesBySymbol = new HashMap<String, List<DblFltPair>>();

			return index -> {
				var last = index - 1;
				var potentialBySymbol = onDateTime.onDateTime(index);
				var potentialBySymbol0 = mutable.value();
				var potentialBySymbol1 = Read.from2(potentialBySymbol).toMap();

				// find out the transactions
				var diffBySymbol = Read
						.from(Union.of(potentialBySymbol0.keySet(), potentialBySymbol1.keySet()))
						.map2(symbol -> {
							var potential0 = potentialBySymbol0.getOrDefault(symbol, 0d);
							var potential1 = potentialBySymbol1.getOrDefault(symbol, 0d);
							return potential1 - potential0;
						})
						.toMap();

				// check on each stock symbol
				for (var e : diffBySymbol.entrySet()) {
					var symbol = e.getKey();
					var diff = e.getValue();
					var bs = Quant.sign(diff);
					var price = dsBySymbol.get(symbol).prices[last];

					var entries0 = entriesBySymbol.getOrDefault(symbol, new ArrayList<>());
					var entries1 = new ArrayList<DblFltPair>();

					Collections.sort(entries0, (pair0, pair1) -> -bs * Float.compare(pair0.t1, pair1.t1));

					for (var entry0 : entries0) {
						var potential0 = entry0.t0;
						var entryPrice = entry0.t1;
						double cancellation;

						// a recent sell would cancel out the highest price buy
						// a recent buy would cancel out the lowest price sell
						if (bs == -1)
							cancellation = min(0, max(diff, -potential0));
						else if (bs == 1)
							cancellation = max(0, min(diff, -potential0));
						else
							cancellation = 0d;

						var potential1 = potential0 + cancellation;
						diff -= cancellation;

						var min = entryPrice * (potential1 < 0 ? stopGain : stopLoss);
						var max = entryPrice * (potential1 < 0 ? stopLoss : stopGain);

						// drop entries that got past their stopping prices
						if (min < price && price < max)
							entries1.add(DblFltPair.of(potential1, entryPrice));
					}

					if (diff != 0d)
						entries1.add(DblFltPair.of(diff, price));

					entriesBySymbol.put(symbol, entries1);
				}

				mutable.update(potentialBySymbol1);

				// re-assemble the entries into current profile
				return Read
						.fromListMap(entriesBySymbol)
						.groupBy(entries -> entries.toDouble(AsDbl.sum(pair -> pair.t0)))
						.toList();
			};
		};
	}

	public default BackAllocator unleverage() {
		var ba0 = this;
		var ba1 = Trade_.isShortSell ? ba0 : ba0.longOnly();
		BackAllocator ba2;

		if (Trade_.leverageAmount < 999999f)
			ba2 = (akds, indices) -> {
				var onDateTime = ba1.allocate(akds, indices);

				return index -> {
					var potentialBySymbol = onDateTime.onDateTime(index);
					var totalPotential = BackAllocatorUtil.totalPotential(potentialBySymbol);
					if (1d < totalPotential)
						return BackAllocatorUtil.scale(potentialBySymbol, 1d / totalPotential);
					else
						return potentialBySymbol;
				};
			};
		else
			ba2 = ba1;

		return ba2;
	}

	public default WalkForwardAllocator walkForwardAllocator() {
		return (akds, index) -> allocate(akds, null).onDateTime(index);
	}

}

class BackAllocatorUtil {

	static List<Pair<String, Double>> scale(List<Pair<String, Double>> potentialBySymbol, double scale) {
		return Read
				.from2(potentialBySymbol)
				.filterValue(potential -> potential != 0d)
				.mapValue(potential -> potential * scale)
				.toList();
	}

	static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).toDouble(AsDbl.sum((symbol, potential) -> potential));
	}

}
