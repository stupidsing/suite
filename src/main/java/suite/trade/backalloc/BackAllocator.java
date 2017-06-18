package suite.trade.backalloc;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.data.DataSource;
import suite.trade.walkforwardalloc.WalkForwardAllocator;

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

	public default BackAllocator unleverage() {
		BackAllocator backAllocator1 = filterShorts();

		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = backAllocator1.allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				double totalPotential = BackAllocatorUtil.totalPotential(potentialBySymbol);
				if (1d < totalPotential)
					return BackAllocatorUtil.scale(potentialBySymbol, 1d / totalPotential);
				else
					return potentialBySymbol;
			};
		};
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
