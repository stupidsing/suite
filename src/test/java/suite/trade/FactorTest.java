package suite.trade;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class FactorTest {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		String factorIndexSymbol = "^GSPC";

		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);
		DataSource ds0 = cfg.dataSource(factorIndexSymbol).range(period); // cfg.dataSource("^GSPC");
		float[] r0 = ts.returns(ds0.prices);

		Streamlet<Asset> assets = cfg.queryCompaniesByMarketCap(Time.now());

		List<Pair<Asset, Double>> pairs = Read.from(assets) //
				.cons(Asset.hsi) //
				.map2(asset -> {
					DataSource ds = cfg.dataSource(asset.symbol).range(period).align(ds0.dates);
					float[] r1 = ts.returns(ds.prices);
					return stat.correlation(r0, r1);
				}) //
				.sortByValue(Object_::compare) //
				.toList();

		for (Pair<Asset, Double> pair : pairs)
			System.out.println(pair);

		BackAllocator ba = (dsBySymbol, times) -> (time, index) -> {
			return new ArrayList<>();
		};

		ba.getClass();
	}

}
