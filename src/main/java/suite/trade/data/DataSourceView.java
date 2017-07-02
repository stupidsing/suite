package suite.trade.data;

import java.util.Map;
import java.util.function.BiFunction;

import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;

public class DataSourceView<K, V> {

	private int tor = 64;
	private Map<String, Map<TimeRange, V>> viewBySymbol;

	public static <K, V> DataSourceView<K, V> of( //
			int tor, //
			int nDays, //
			int alignment, //
			Streamlet2<String, DataSource> dsByKey, //
			long[] ts, //
			BiFunction<DataSource, TimeRange, V> fun) {
		return new DataSourceView<>(tor, nDays, alignment, dsByKey, ts, fun);
	}

	private DataSourceView( //
			int tor, //
			int nDays, //
			int alignment, //
			Streamlet2<String, DataSource> dsByKey, //
			long[] ts, //
			BiFunction<DataSource, TimeRange, V> fun) {
		this.viewBySymbol = dsByKey //
				.map2((symbol, ds) -> TimeRange //
						.rangeOf(ts) //
						.addDays(-tor) //
						.backTestDaysBefore(nDays, alignment) //
						.map2(period -> fun.apply(ds, period)) //
						.toMap()) //
				.toMap();
	}

	public V get(String symbol, Time time) {
		TimeRange mrsPeriod = TimeRange.backTestDaysBefore(time.addDays(-tor), 256, 32);
		Map<TimeRange, V> m = viewBySymbol.get(symbol);
		return m != null ? m.get(mrsPeriod) : null;
	}

}
