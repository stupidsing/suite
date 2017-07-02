package suite.trade.backalloc;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.walkforwardalloc.WalkForwardAllocator;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.String_;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface BackAllocator {

	public OnDateTime allocate(Streamlet2<String, DataSource> dsBySymbol, long[] ts);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential
		 *         values, or null if the strategy do not want to trade on that
		 *         date. The assets will be allocated according to potential
		 *         values pro-rata.
		 */
		public List<Pair<String, Double>> onDateTime(Time time, int index);
	}

	public default BackAllocConfiguration bac(Fun<Time, Streamlet<Asset>> assetsFun) {
		return new BackAllocConfiguration(assetsFun, this);
	}

	public default BackAllocConfiguration bacUnl(Fun<Time, Streamlet<Asset>> assetsFun) {
		return unleverage().bac(assetsFun);
	}

	public default BackAllocator dump() {
		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> {
				List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(time, index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public default BackAllocator even() {
		BackAllocator ba1 = filterShorts();

		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = ba1.allocate(dsBySymbol, ts);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				int size = Read.from2(potentialBySymbol).size();

				if (0 < size) {
					double each = 1d / size;
					return Read.from2(potentialBySymbol) //
							.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
							.mapValue(potential -> 1d / each) //
							.toList();
				} else
					return Collections.emptyList();
			};
		};
	}

	public default BackAllocator filterAssets(Predicate<String> pred) {
		return (dsBySymbol, ts) -> allocate(dsBySymbol.filterKey(pred), ts)::onDateTime;
	}

	public default BackAllocator filterByIndex(Configuration cfg) {
		return filterByIndex(cfg, "^GSPC");
	}

	public default BackAllocator filterByIndex(Configuration cfg, String indexSymbol) {
		DataSource indexDataSource = cfg.dataSource(indexSymbol);

		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> {
				Time date = time.date();
				long t0 = date.addDays(-7).epochSec();
				long tx = date.epochSec();
				DataSource ids = indexDataSource.range(t0, tx);

				double indexPrice0 = ids.get(-1).t1;
				double indexPricex = ids.get(-2).t1;
				double indexReturn = (indexPricex - indexPrice0) / indexPrice0;

				return -.03f < indexReturn //
						? onDateTime.onDateTime(time, index) //
						: Collections.emptyList();
			};
		};
	}

	public default BackAllocator filterShorts() {
		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);

				return Read.from2(potentialBySymbol) //
						.map2((symbol, potential) -> {
							if (Double.isFinite(potential))
								return potential;
							else
								throw new RuntimeException("potential is " + potential);
						}) //
						.filterValue(potential -> 0d < potential) //
						.toList();
			};
		};
	}

	public default BackAllocator frequency(int tradeFrequency) {
		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return new OnDateTime() {
				private Time time0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDateTime(Time time_, int index) {
					Time time1 = time_.addDays(-(time_.epochDay() % tradeFrequency));
					if (!Objects.equals(time0, time1)) {
						time0 = time1;
						result0 = onDateTime.onDateTime(time1, index);
					}
					return result0;
				}
			};
		};
	}

	public default BackAllocator holdMinimum(int period) {
		return (dsBySymbol, ts) -> {
			Deque<Map<String, Double>> queue = new ArrayDeque<>();
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> {
				Map<String, Double> ratioBySymbol = Read //
						.from2(onDateTime.onDateTime(time, index)) //
						.toMap();

				queue.addLast(ratioBySymbol);
				while (period < queue.size())
					queue.removeFirst();

				Map<String, Double> max = new HashMap<>();

				for (Map<String, Double> m : queue)
					for (Entry<String, Double> e : m.entrySet())
						max.compute(e.getKey(), (k, v) -> Math.max(v != null ? v : 0d, e.getValue()));

				return Read.from2(max).toList();
			};
		};
	}

	public default BackAllocator reallocate() {
		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				return BackAllocatorUtil.scale(potentialBySymbol, 1d / BackAllocatorUtil.totalPotential(potentialBySymbol));
			};
		};
	}

	public default BackAllocator relative(DataSource indexDataSource) {
		return (dsBySymbol0, times_) -> {
			Streamlet2<String, DataSource> dsBySymbol1 = dsBySymbol0 //
					.mapValue(ds0 -> {
						long[] times = ds0.ts;
						float[] prices = ds0.prices;
						long[] indexDates = indexDataSource.ts;
						float[] indexPrices = indexDataSource.prices;
						int length = times.length;
						int indexLength = indexDates.length;
						float[] prices1 = new float[length];
						int ii = 0;

						for (int di = 0; di < length; di++) {
							long date = times[di];
							while (ii < indexLength && indexDates[ii] < date)
								ii++;
							prices1[di] = prices[di] / indexPrices[ii];
						}

						return new DataSource(times, prices1);
					}) //
					.collect(As::streamlet2);

			return allocate(dsBySymbol1, times_)::onDateTime;
		};
	}

	public default BackAllocator relativeToHsi(Configuration cfg) {
		return relativeToIndex(cfg, "^HSI");
	}

	public default BackAllocator relativeToIndex(Configuration cfg, String indexSymbol) {
		return relative(cfg.dataSource(indexSymbol));
	}

	public default BackAllocator top(int top) {
		return (dsBySymbol, ts) -> {
			OnDateTime onDateTime = allocate(dsBySymbol, ts);

			return (time, index) -> Read //
					.from2(onDateTime.onDateTime(time, index)) //
					.sortByValue((r0, r1) -> Object_.compare(r1, r0)) //
					.take(top) //
					.toList();
		};
	}

	public default BackAllocator unleverage() {
		BackAllocator ba0 = this;
		BackAllocator ba1 = Trade_.isShortSell ? ba0 : ba0.filterShorts();
		BackAllocator ba2;

		if (Trade_.leverageAmount < 999999f)
			ba2 = (dsBySymbol, ts) -> {
				OnDateTime onDateTime = ba1.allocate(dsBySymbol, ts);

				return (time, index) -> {
					List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
					double totalPotential = BackAllocatorUtil.totalPotential(potentialBySymbol);
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
		return (dsBySymbol, index) -> allocate(dsBySymbol, null).onDateTime(null, index);
	}

}

class BackAllocatorUtil {

	static List<Pair<String, Double>> scale(List<Pair<String, Double>> potentialBySymbol, double scale) {
		return Read.from2(potentialBySymbol) //
				.filterValue(potential -> potential != 0d) //
				.mapValue(potential -> potential * scale) //
				.toList();
	}

	static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).collectAsDouble(ObjObj_Dbl.sum((symbol, potential) -> potential));
	}

}
