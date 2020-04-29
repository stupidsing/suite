package suite.trade.data;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Map;

import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource.AlignKeyDataSource;

public class DataSourceView<K, V> {

	public interface DataSourceViewFun<K, V> {
		public V apply(K key, DataSource ds, TimeRange period);
	}

	private int tor;
	private int nLookbackDays;
	private int alignment;
	private AlignKeyDataSource<K> akds;
	private Map<K, Map<TimeRange, V>> viewByKey;

	public static <K, V> DataSourceView<K, V> of(
			int tor,
			int nLookbackDays,
			AlignKeyDataSource<K> akds,
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
		return new DataSourceView<>(tor, nLookbackDays, alignment, akds, fun);
	}

	private DataSourceView(
			int tor,
			int nLookbackDays,
			int alignment,
			AlignKeyDataSource<K> akds,
			DataSourceViewFun<K, V> fun) {
		var fr = TimeRange.min.epochSec();
		var to = TimeRange.max.epochSec();

		for (var t : akds.ts) {
			fr = min(t, fr);
			to = max(t, to);
		}

		var period = TimeRange.of(Time.ofEpochSec(fr), Time.ofEpochSec(to).addDays(1));

		this.tor = tor;
		this.nLookbackDays = nLookbackDays;
		this.alignment = alignment;
		this.akds = akds;
		this.viewByKey = akds.dsByKey
				.map2((key, ds) -> period
						.addDays(-tor)
						.backTestDaysBefore(nLookbackDays, alignment)
						.map2(period_ -> fun.apply(key, ds, period_))
						.toMap())
				.toMap();
	}

	public V get(String symbol, int index) {
		var period = period(index);
		var m = viewByKey.get(symbol);
		return m != null ? m.get(period) : null;
	}

	public TimeRange period(int index) {
		return TimeRange.backTestDaysBefore(Time.ofEpochSec(akds.ts[index - 1]).addDays(-tor), nLookbackDays, alignment);
	}

}
