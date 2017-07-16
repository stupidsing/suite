package suite.trade.backalloc;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.math.stat.Quant;
import suite.primitive.DblDbl_Dbl;
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
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Datum;
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

	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential values, or
		 *         null if the strategy do not want to trade on that date. The assets
		 *         will be allocated according to potential values pro-rata.
		 */
		public List<Pair<String, Double>> onDateTime(int index);
	}

	public default BackAllocator byTime(IntPredicate monthPred) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);
			return index -> monthPred.test(Time.ofEpochSec(akds.ts[index]).month()) //
					? onDateTime.onDateTime(index) //
					: Collections.emptyList();
		};
	}

	public default BackAllocConfiguration cfg(Fun<Time, Streamlet<Asset>> assetsFun) {
		return new BackAllocConfiguration(assetsFun, this);
	}

	public default BackAllocConfiguration cfgUnl(Fun<Time, Streamlet<Asset>> assetsFun) {
		return pick(40).unleverage().cfg(assetsFun);
	}

	public default BackAllocator dump() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public default BackAllocator even() {
		BackAllocator ba1 = longOnly();

		return (akds, indices) -> {
			OnDateTime onDateTime = ba1.allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
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

	public default BackAllocator filterByAsset(Predicate<String> pred) {
		return (akds0, indices) -> {
			AlignKeyDataSource<String> akds1 = new AlignKeyDataSource<>(akds0.ts, akds0.dsByKey.filterKey(pred));
			return allocate(akds1, indices)::onDateTime;
		};
	}

	public default BackAllocator filterByIndex(Configuration cfg) {
		return filterByIndexReturn(cfg, "^GSPC");
	}

	public default BackAllocator filterByIndexReturn(Configuration cfg, String indexSymbol) {
		DataSource indexDataSource = cfg.dataSource(indexSymbol);

		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				Time date = Time.ofEpochSec(akds.ts[index]).date();
				long t0 = date.addDays(-7).epochSec();
				long tx = date.epochSec();
				DataSource ids = indexDataSource.range(t0, tx);

				double indexPrice0 = ids.get(-1).t1;
				double indexPricex = ids.get(-2).t1;
				double indexReturn = Quant.return_(indexPrice0, indexPricex);

				return -.03f < indexReturn //
						? onDateTime.onDateTime(index) //
						: Collections.emptyList();
			};
		};
	}

	public default BackAllocator frequency(int freq) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return new OnDateTime() {
				private Time time0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDateTime(int index) {
					Time time_ = Time.ofEpochSec(akds.ts[index]);
					Time time1 = time_.addDays(-(time_.epochDay() % freq));
					if (!Objects.equals(time0, time1)) {
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
			Deque<Map<String, Double>> queue = new ArrayDeque<>();
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				Map<String, Double> ratioBySymbol = Read //
						.from2(onDateTime.onDateTime(index)) //
						.toMap();

				queue.addLast(ratioBySymbol);
				while (period < queue.size())
					queue.removeFirst();

				Map<String, Double> map = new HashMap<>();

				for (Map<String, Double> m : queue)
					for (Entry<String, Double> e : m.entrySet())
						map.compute(e.getKey(), (k, v) -> fun.apply(v != null ? v : 0d, e.getValue()));

				return Read.from2(map).toList();
			};
		};
	}

	public default BackAllocator january() {
		IntPredicate monthPred = month -> month == 1;
		return byTime(monthPred);
	}

	public default BackAllocator longOnly() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);

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

	public default BackAllocator pick(int top) {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> Read //
					.from2(onDateTime.onDateTime(index)) //
					.sortByValue((r0, r1) -> Object_.compare(r1, r0)) //
					.take(top) //
					.toList();
		};
	}

	public default BackAllocator reallocate() {
		return (akds, indices) -> {
			OnDateTime onDateTime = allocate(akds, indices);

			return index -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
				return BackAllocatorUtil.scale(potentialBySymbol, 1d / BackAllocatorUtil.totalPotential(potentialBySymbol));
			};
		};
	}

	public default BackAllocator relative(DataSource indexDataSource) {
		return (akds0, times_) -> {
			Streamlet2<String, DataSource> dsBySymbol1 = akds0.dsByKey //
					.mapValue(ds0 -> {
						float[] indexPrices = indexDataSource.alignBeforePrices(ds0.ts).prices;
						int length = ds0.ts.length;
						Datum[] data1 = new Datum[length];

						for (int i = 0; i < length; i++) {
							double r = 1d / indexPrices[i];
							data1[i] = new Datum( //
									ds0.ts[i], //
									ds0.ts[i] + DataSource.tickDuration, //
									(float) (ds0.opens[i] * r), //
									(float) (ds0.closes[i] * r), //
									(float) (ds0.lows[i] * r), //
									(float) (ds0.highs[i] * r), //
									ds0.volumes[i]);
						}

						return DataSource.of(Read.from(data1));
					}) //
					.collect(As::streamlet2);

			return allocate(new AlignKeyDataSource<>(akds0.ts, dsBySymbol1), times_)::onDateTime;
		};
	}

	public default BackAllocator relativeToHsi(Configuration cfg) {
		return relativeToIndex(cfg, "^HSI");
	}

	public default BackAllocator relativeToIndex(Configuration cfg, String indexSymbol) {
		return relative(cfg.dataSource(indexSymbol));
	}

	public default BackAllocator sellInMay() {
		return byTime(month -> month < 5 || 11 <= month);
	}

	public default BackAllocator unleverage() {
		BackAllocator ba0 = this;
		BackAllocator ba1 = Trade_.isShortSell ? ba0 : ba0.longOnly();
		BackAllocator ba2;

		if (Trade_.leverageAmount < 999999f)
			ba2 = (akds, indices) -> {
				OnDateTime onDateTime = ba1.allocate(akds, indices);

				return index -> {
					List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(index);
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
		return (akds, index) -> allocate(akds, null).onDateTime(index);
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
