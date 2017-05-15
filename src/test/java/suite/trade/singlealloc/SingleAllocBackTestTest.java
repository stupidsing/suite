package suite.trade.singlealloc;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Forex;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;

public class SingleAllocBackTestTest {

	private DatePeriod period = DatePeriod.fiveYears();
	private Configuration cfg = new ConfigurationImpl();

	@Test
	public void testBackTestForex() {
		new Forex().invertedCurrencies.sink(this::backTest);
	}

	@Test
	public void testBackTestHkex() {
		for (String code : Arrays.asList( //
				"0004", //
				"0020", //
				"0175", //
				"0322", //
				"1128", //
				"1169", //
				"1357", //
				"2018"))
			backTest(code + ".HK", code);
	}

	@Test
	public void testBackTestHkexDetails() {
		backTest(cfg.queryCompany("0005.HK"));
	}

	private void backTest(Asset asset) {
		backTest(asset.symbol, asset.toString()) //
				.forEach((sn, backTest) -> {
					String conclusion = backTest.concludeLog.toString();
					LogUtil.info("BEGIN strategy = " + sn + conclusion);
					LogUtil.info(backTest.tradeLog.toString());
					LogUtil.info("END__ strategy = " + sn + conclusion);
				});
	}

	private Map<String, SingleAllocBackTest> backTest(String code, String disp) {
		Strategos sr = new Strategos();
		DataSource ds = cfg.dataSource(code, period);

		return Read //
				.<String, BuySellStrategy> empty2() //
				.cons("longHold", sr.longHold) //
				.cons("lowPassPrediction", sr.lowPassPrediction(128, 8, 8, .02f)) //
				.cons("movingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f)) //
				.cons("macdSignalLineX", sr.macdSignalLineX(.8f, .9f, .85f)) //
				.cons("macdZeroLineX", sr.macdZeroLineX(.8f, .9f)) //
				.map2((sn, strategy) -> sn, (sn, strategy) -> backTest_(ds, disp + ", strategy = " + sn, strategy)) //
				.toMap();
	}

	private SingleAllocBackTest backTest_(DataSource ds, String disp, BuySellStrategy strategy) {
		SingleAllocBackTest backTest = SingleAllocBackTest.test(ds, strategy);
		LogUtil.info(disp + backTest.concludeLog);
		return backTest;
	}

}
