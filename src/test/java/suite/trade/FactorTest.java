package suite.trade;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class FactorTest {

	private Configuration cfg = new ConfigurationImpl();
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		Streamlet<Asset> assets0 = cfg.queryCompaniesByMarketCap(Time.now());
		Streamlet<Asset> assets1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		List<Pair<Asset, Double>> pairs = test( //
				Read.each("^DJI", "^GSPC", "NDAQ"), //
				Streamlet.concat(assets0, assets1) //
						.cons(Asset.hsi) //
						.cons(cfg.queryCompany("0753.HK")) //
						.distinct());

		for (Pair<Asset, Double> pair : pairs)
			System.out.println(pair);

		BackAllocator ba = (dsBySymbol, times) -> {
			return (time, index) -> {
				return new ArrayList<>();
			};
		};

		ba.getClass();
	}

	private List<Pair<Asset, Double>> test(Streamlet<String> indices, Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);

		Streamlet2<String, DataSource> dsBySymbol = indices //
				.map2(symbol -> cfg.dataSource(symbol).range(period)) //
				.collect(As::streamlet2);

		AlignKeyDataSource<String> akds = DataSource.alignAll(dsBySymbol);

		float[] indexReturns = akds.dsByKey //
				.map((symbol, ds) -> ts.returns(ds.prices)) //
				.fold(new float[akds.ts.length], mtx::add);

		return assets //
				.map2(asset -> {
					DataSource ds = cfg.dataSource(asset.symbol).range(period).align(akds.ts);
					float[] returns = ts.returns(ds.prices);
					return stat.correlation(indexReturns, returns);
				}) //
				.sortByValue(Object_::compare) //
				.toList();
	}

}
