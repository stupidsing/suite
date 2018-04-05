package suite.trade.analysis;

import java.util.Map;

import org.junit.Test;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Usex;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;

public class FactorLrTest {

	private Configuration cfg = new ConfigurationImpl();

	@Test
	public void test() {
		var indices = Read.each(Usex.crudeOil, Usex.dowJones, Usex.nasdaq, Usex.sp500);

		var assets0 = cfg.queryCompaniesByMarketCap(Time.now());
		var assets1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		var assets = Streamlet //
				.concat(assets0, assets1) //
				.cons(Asset.hsi) //
				.cons(cfg.queryCompany("0753.HK")) //
				.distinct();

		Map<Asset, String> pairs = FactorLr.of(cfg, indices).query(assets);

		for (var pair : pairs.entrySet())
			System.out.println(pair);
	}

}
