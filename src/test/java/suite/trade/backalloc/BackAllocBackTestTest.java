package suite.trade.backalloc;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.BackTester;
import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator0;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Sink;

public class BackAllocBackTestTest {

	private float initial = 1000000f;
	private TimeRange period = TimeRange.ofYear(2017);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new ConfigurationImpl();
	private BackTester runner = new BackTester();

	@Test
	public void testBackTest() {
		BackAllocator backAllocator = MovingAvgMeanReversionBackAllocator0.of(log);
		Simulate sim = runner.backTest(backAllocator, period);
		SummarizeByStrategy<String> sbs = Summarize.of(cfg, Read.from(sim.trades)).summarize(trade -> trade.symbol);
		System.out.println(sbs.log);
		assertGrowth(out(sim));
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		BackAllocator backAllocator = MovingAvgMeanReversionBackAllocator.of(log);
		assertGrowth(out(runner.backTest(backAllocator, period, Read.each(asset))));
	}

	private void assertGrowth(Simulate sim) {
		float[] valuations = sim.valuations;
		int last = valuations.length - 1;
		double r = Trade_.riskFreeInterestRate(last);
		assertTrue(initial * r < valuations[last]);
	}

	private Simulate out(Simulate sim) {
		System.out.println(sim.conclusion());
		return sim;
	}

}
