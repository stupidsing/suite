package suite.trade;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class FactorTest {

	private Configuration cfg = new ConfigurationImpl();
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		Streamlet<String> indices = Read.each("^DJI", "^GSPC", "NDAQ");
		Streamlet<Asset> assets0 = cfg.queryCompaniesByMarketCap(Time.now());
		Streamlet<Asset> assets1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		AlignKeyDataSource<String> akds = indices //
				.map2(cfg::dataSource) //
				.collect(As::streamlet2) //
				.apply(DataSource::alignAll);

		float[] indexReturns = akds.dsByKey //
				.map((symbol, ds) -> ts.returns(ds.prices)) //
				.fold(new float[akds.ts.length], mtx::add);

		DataSource irds = new DataSource(akds.ts, indexReturns);

		List<Pair<Asset, Double>> pairs = test( //
				irds, //
				Streamlet.concat(assets0, assets1) //
						.cons(Asset.hsi) //
						.cons(cfg.queryCompany("0753.HK")) //
						.distinct());

		for (Pair<Asset, Double> pair : pairs)
			System.out.println(pair);

		backAllocator(irds).getClass();
	}

	private BackAllocator backAllocator(DataSource irds) {
		return (dsBySymbol, ts_) -> {
			Map<String, DataSource> returnDsBySymbol = dsBySymbol.mapValue(this::returns).toMap();

			DataSourceView<String, Double> dsv = DataSourceView.of(0, 64, dsBySymbol, ts_,
					(symbol, ds, period) -> correlate(irds, returnDsBySymbol.get(symbol), period));

			return (time, index) -> {
				float indexReturn = irds.last(time).t1;

				return dsBySymbol //
						.map2((symbol, ds) -> indexReturn * dsv.get(symbol, time)) //
						.toList();
			};
		};
	}

	private List<Pair<Asset, Double>> test(DataSource irds, Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);

		return assets //
				.map2(asset -> correlate(irds, returns(cfg.dataSource(asset.symbol)), period)) //
				.sortByValue(Object_::compare) //
				.toList();
	}

	private DataSource returns(DataSource ds) {
		return new DataSource(ds.ts, ts.returns(ds.prices));
	}

	private double correlate(DataSource irds0, DataSource rds0, TimeRange period) {
		DataSource irds1 = irds0.range(period);
		DataSource rds1 = rds0.range(period).align(irds1.ts);
		return stat.correlation(irds1.prices, rds1.prices);
	}

}
