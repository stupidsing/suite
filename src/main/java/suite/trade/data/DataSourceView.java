package suite.trade.data;

import java.util.Map;

import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;

public class DataSourceView<K, V> {

	public interface DataSourceViewFun<K, V> {
		public V apply(String symbol, DataSource ds, TimeRange period);
	}

	private int tor;
	private int nDays;
	private int alignment;
	private Map<String, Map<TimeRange, V>> viewBySymbol;

	public static <K, V> DataSourceView<K, V> of( //
			int tor, //
			Streamlet2<String, DataSource> dsByKey, //
			long[] ts, //
			DataSourceViewFun<K, V> fun) {
		return of(tor, 256, dsByKey, ts, fun);
	}

	public static <K, V> DataSourceView<K, V> of( //
			int tor, //
			int nDays, //
			Streamlet2<String, DataSource> dsByKey, //
			long[] ts, //
			DataSourceViewFun<K, V> fun) {
		int alignment;
		if (nDays <= 1)
			alignment = 1;
		else if (nDays <= 4)
			alignment = 2;
		else if (nDays <= 16)
			alignment = 4;
		else if (nDays <= 64)
			alignment = 8;
		else if (nDays <= 256)
			alignment = 16;
		else
			alignment = 32;
		return new DataSourceView<>(tor, nDays, alignment, dsByKey, ts, fun);
	}

	private DataSourceView( //
			int tor, //
			int nDays, //
			int alignment, //
			Streamlet2<String, DataSource> dsByKey, //
			long[] ts, //
			DataSourceViewFun<K, V> fun) {
		this.tor = tor;
		this.nDays = nDays;
		this.alignment = alignment;
		this.viewBySymbol = dsByKey //
				.map2((symbol, ds) -> TimeRange //
						.rangeOf(ts) //
						.addDays(-tor) //
						.backTestDaysBefore(nDays, alignment) //
						.map2(period -> fun.apply(symbol, ds, period)) //
						.toMap()) //
				.toMap();
	}

	public V get(String symbol, Time time) {
		TimeRange period = period(time);
		Map<TimeRange, V> m = viewBySymbol.get(symbol);
		return m != null ? m.get(period) : null;
	}

	public TimeRange period(Time time) {
		return TimeRange.backTestDaysBefore(time.addDays(-tor), nDays, alignment);
	}

}
