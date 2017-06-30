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
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignDataSource;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class FactorTest {

	private Configuration cfg = new ConfigurationImpl();
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);

		Streamlet<DataSource> dataSources = Read //
				.each("^DJI", "^GSPC", "NDAQ") //
				.map(symbol -> cfg.dataSource(symbol).range(period)) //
				.collect(As::streamlet);

		AlignDataSource alignDataSource = DataSource.alignAll(dataSources);

		float[] indexReturns = dataSources //
				.map(ds -> ts.returns(alignDataSource.align(ds).prices)) //
				.fold(new float[alignDataSource.ts.length], (r0, r1) -> mtx.add(r0, r1));

		Streamlet<Asset> assets = cfg.queryCompaniesByMarketCap(Time.now());

		List<Pair<Asset, Double>> pairs = Read.from(assets) //
				.cons(Asset.hsi) //
				.cons(cfg.queryCompany("1169.HK")) //
				.cons(cfg.queryCompany("2638.HK")) //
				.cons(cfg.queryCompany("0880.HK")) //
				.map2(asset -> {
					DataSource ds = cfg.dataSource(asset.symbol).range(period).align(alignDataSource.ts);
					float[] returns = ts.returns(ds.prices);
					return stat.correlation(indexReturns, returns);
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
