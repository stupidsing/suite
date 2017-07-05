package suite.trade.analysis;

import java.util.List;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;

public class FactorTest {

	private Configuration cfg = new ConfigurationImpl();

	@Test
	public void test() {
		Streamlet<Asset> assets0 = cfg.queryCompaniesByMarketCap(Time.now());
		Streamlet<Asset> assets1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		Streamlet<Asset> assets = Streamlet.concat(assets0, assets1) //
				.cons(Asset.hsi) //
				.cons(cfg.queryCompany("0753.HK")) //
				.distinct();

		List<Pair<Asset, Double>> pairs = Factor.ofCrudeOil(cfg).query(assets);

		for (Pair<Asset, Double> pair : pairs)
			System.out.println(pair);
	}

}
