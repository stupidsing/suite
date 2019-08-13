package suite.trade.analysis;

import org.junit.Test;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.Usex;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

public class FactorLrTest {

	private TradeCfg cfg = new TradeCfgImpl();

	@Test
	public void test() {
		var indices = Read.each(Usex.crudeOil, Usex.dowJones, Usex.nasdaq, Usex.sp500);

		var instruments0 = cfg.queryCompaniesByMarketCap(Time.now());
		var instruments1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		var instruments = Streamlet //
				.concat(instruments0, instruments1) //
				.cons(Instrument.hsi) //
				.cons(cfg.queryCompany("0753.HK")) //
				.distinct();

		var pairs = new FactorLr(cfg, indices).query(instruments);

		for (var pair : pairs.entrySet())
			System.out.println(pair);
	}

}
