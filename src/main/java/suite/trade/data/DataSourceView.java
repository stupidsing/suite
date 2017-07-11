package suite.trade.data;

import java.util.Map;

import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;

public class DataSourceView<K, V> {

	public interface DataSourceViewFun<K, V> {
		public V apply(K key, DataSource ds, TimeRange period);
	}

	private int tor;
	private int nLookbackDays;
	private int alignment;
	private Map<K, Map<TimeRange, V>> viewByKey;

	public static <K, V> DataSourceView<K, V> of( //
			int tor, //
			Streamlet2<K, DataSource> dsByKey, //
			int[] indices, //
			DataSourceViewFun<K, V> fun) {
		return of(tor, 256, dsByKey, indices, fun);
	}

	public static <K, V> DataSourceView<K, V> of( //
			int tor, //
			int nLookbackDays, //
			Streamlet2<K, DataSource> dsByKey, //
			int[] indices, //
			DataSourceViewFun<K, V> fun) {
		int alignment;
		if (nLookbackDays <= 1)
			alignment = 1;
		else if (nLookbackDays <= 4)
			alignment = 2;
		else if (nLookbackDays <= 16)
			alignment = 4;
		else if (nLookbackDays <= 64)
			alignment = 8;
		else if (nLookbackDays <= 256)
			alignment = 16;
		else
			alignment = 32;
		return new DataSourceView<>(tor, nLookbackDays, alignment, dsByKey, indices, fun);
	}

	private DataSourceView( //
			int tor, //
			int nLookbackDays, //
			int alignment, //
			Streamlet2<K, DataSource> dsByKey, //
			int[] indices, //
			DataSourceViewFun<K, V> fun) {
		long fr = TimeRange.min.epochSec();
		long to = TimeRange.max.epochSec();

		for (DataSource ds : dsByKey.values()) {
			long[] ts = ds.ts;
			for (int index : indices) {
				long t = ts[index];
				fr = Long.min(t, fr);
				to = Long.max(t, to);
			}
		}

		TimeRange period0 = TimeRange.of(Time.ofEpochSec(fr), Time.ofEpochSec(to).addDays(1));

		this.tor = tor;
		this.nLookbackDays = nLookbackDays;
		this.alignment = alignment;
		this.viewByKey = dsByKey //
				.map2((key, ds) -> period0 //
						.addDays(-tor) //
						.backTestDaysBefore(nLookbackDays, alignment) //
						.map2(period -> fun.apply(key, ds, period)) //
						.toMap()) //
				.toMap();
	}

	public V get(String symbol, Time time) {
		TimeRange period = period(time);
		Map<TimeRange, V> m = viewByKey.get(symbol);
		return m != null ? m.get(period) : null;
	}

	public TimeRange period(Time time) {
		return TimeRange.backTestDaysBefore(time.addDays(-tor), nLookbackDays, alignment);
	}

}
