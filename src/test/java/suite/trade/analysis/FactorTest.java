package suite.trade.analysis;

import org.junit.Test;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
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

		var pairs = Factor.of(cfg, indices).query(instruments);
		pairs.forEach(System.out::println);
	}

}
