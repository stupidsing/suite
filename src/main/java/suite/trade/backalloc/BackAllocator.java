package suite.trade.backalloc;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.walkforwardalloc.WalkForwardAllocator;
import suite.util.String_;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface BackAllocator {

	public OnDateTime allocate(Streamlet2<String, DataSource> dataSourceBySymbol, List<Time> times);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential
		 *         values, or null if the strategy do not want to trade on that
		 *         date. The assets will be allocated according to potential
		 *         values pro-rata.
		 */
		public List<Pair<String, Double>> onDateTime(Time time, int index);
	}

	public default BackAllocator byTradeFrequency(int tradeFrequency) {
		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = allocate(dataSourceBySymbol, times);

			return new OnDateTime() {
				private Time date0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDateTime(Time time0, int index) {
					Time time1 = time0.addDays(-time0.epochDay() % tradeFrequency);
					if (!Objects.equals(date0, time1)) {
						date0 = time1;
						return result0 = onDateTime.onDateTime(time1, index);
					} else
						return result0;
				}
			};
		};
	}

	public default BackAllocator dump() {
		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(time, index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public default BackAllocator even() {
		BackAllocator ba1 = filterShorts();

		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = ba1.allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				double each = 1d / Read.from2(potentialBySymbol).size();

				return Read.from2(potentialBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
						.mapValue(potential -> 1d / each) //
						.toList();
			};
		};
	}

	public default BackAllocator filterAssets(Predicate<String> pred) {
		return (dataSourceBySymbol, times) -> allocate(dataSourceBySymbol.filterKey(pred), times)::onDateTime;
	}

	public default BackAllocator filterShorts() {
		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = allocate(dataSourceBySymbol, times);

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

	public default BackAllocator reallocate() {
		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				return BackAllocatorUtil.scale(potentialBySymbol, 1d / BackAllocatorUtil.totalPotential(potentialBySymbol));
			};
		};
	}

	public default BackAllocator relative(DataSource indexDataSource) {
		return (dataSourceBySymbol0, times_) -> {
			Streamlet2<String, DataSource> dataSourceBySymbol1 = dataSourceBySymbol0 //
					.mapValue(dataSource0 -> {
						String[] times = dataSource0.dates;
						float[] prices = dataSource0.prices;
						String[] indexDates = indexDataSource.dates;
						float[] indexPrices = indexDataSource.prices;
						int length = times.length;
						int indexLength = indexDates.length;
						float[] prices1 = new float[length];
						int ii = 0;

						for (int di = 0; di < length; di++) {
							String date = times[di];
							while (ii < indexLength && indexDates[ii].compareTo(date) < 0)
								ii++;
							prices1[di] = prices[di] / indexPrices[ii];
						}

						return new DataSource(times, prices1);
					}) //
					.collect(As::streamlet2);

			return allocate(dataSourceBySymbol1, times_)::onDateTime;
		};
	}

	public default BackAllocator relativeToHsi(Configuration cfg) {
		return relativeToIndex(cfg, "^HSI");
	}

	public default BackAllocator relativeToIndex(Configuration cfg, String indexSymbol) {
		return relative(cfg.dataSource(indexSymbol));
	}

	public default BackAllocator unleverage() {
		BackAllocator ba1 = Trade_.isShortSell ? this : filterShorts();
		BackAllocator ba2;

		if (Trade_.maxLeverageAmount < 999999f)
			ba2 = (dataSourceBySymbol, times) -> {
				OnDateTime onDateTime = ba1.allocate(dataSourceBySymbol, times);

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
		return (dataSourceBySymbol, index) -> allocate(dataSourceBySymbol, null).onDateTime(null, index);
	}

}

class BackAllocatorUtil {

	static List<Pair<String, Double>> scale(List<Pair<String, Double>> potentialBySymbol, double scale) {
		return Read.from2(potentialBySymbol) //
				.mapValue(potential -> potential * scale) //
				.toList();
	}

	static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).collectAsDouble(As.sumOfDoubles((symbol, potential) -> potential));
	}

}
