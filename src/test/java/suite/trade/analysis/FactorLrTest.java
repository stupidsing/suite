package suite.trade.analysis;

import org.junit.Test;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Usex;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

public class FactorLrTest {

	private TradeCfg cfg = new TradeCfgImpl();

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

		var pairs = FactorLr.of(cfg, indices).query(assets);

		for (var pair : pairs.entrySet())
			System.out.println(pair);
	}

}
