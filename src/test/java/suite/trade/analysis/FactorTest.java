package suite.trade.analysis;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.Usex;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

public class FactorTest {

	private TradeCfg cfg = new TradeCfgImpl();

	@Test
	public void test() {
		var indices = Read.each(Usex.crudeOil);

		var instruments0 = cfg.queryCompaniesByMarketCap(Time.now());
		var instruments1 = cfg.queryHistory().map(trade -> trade.symbol).distinct().map(cfg::queryCompany);

		var instruments = Streamlet //
				.concat(instruments0, instruments1) //
				.cons(Instrument.hsi) //
				.cons(cfg.queryCompany("0753.HK")) //
				.distinct();

		new Factor(cfg, indices).query(instruments).forEach(System.out::println);
	}

}
