package suite.trade.backalloc;

import org.junit.jupiter.api.Test;
import primal.MoreVerbs.Read;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.run.BackTester;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator;
import suite.trade.backalloc.strategy.PmamrBackAllocator;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackAllocBackTestTest {

	private float initial = 1000000f;
	private TimeRange period = TimeRange.ofYear(2017);

	private TradeCfg cfg = new TradeCfgImpl();
	private BackTester runner = new BackTester();

	@Test
	public void testBackTest() {
		var backAllocator = new PmamrBackAllocator().backAllocator();
		var sim = runner.backTest(backAllocator, period);
		var sbs = Summarize.of(cfg, Read.from(sim.trades)).summarize(trade -> trade.symbol);
		System.out.println(sbs.log);
		assertGrowth(out(sim));
	}

	@Test
	public void testBackTestSingle() {
		var instrument = cfg.queryCompany("0945.HK");
		var backAllocator = new MovingAvgMeanReversionBackAllocator().backAllocator();
		assertGrowth(out(runner.backTest(backAllocator, period, Read.each(instrument))));
	}

	private void assertGrowth(Simulate sim) {
		var valuations = sim.valuations;
		var last = valuations.length - 1;
		var r = Trade_.riskFreeInterestRate(last);
		assertTrue(initial * r < valuations[last]);
	}

	private Simulate out(Simulate sim) {
		System.out.println(sim.conclusion());
		return sim;
	}

}
