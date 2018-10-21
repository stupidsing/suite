package suite.trade.singlealloc;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Forex;
import suite.trade.Instrument;
import suite.trade.TimeRange;
import suite.trade.data.DataSource;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

public class SingleAllocBackTestTest {

	private TimeRange period = TimeRange.fiveYears();
	private TradeCfg cfg = new TradeCfgImpl();

	@Test
	public void testBackTestForex() {
		new Forex().invertedCurrencies.sink(this::backTest);
	}

	@Test
	public void testBackTestHkex() {
		for (var code : List.of( //
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

	private void backTest(Instrument instrument) {
		backTest(instrument.symbol, instrument.toString()) //
				.forEach((sn, backTest) -> {
					var conclusion = backTest.concludeLog.toString();
					LogUtil.info("BEGIN strategy = " + sn + conclusion);
					LogUtil.info(backTest.tradeLog.toString());
					LogUtil.info("END__ strategy = " + sn + conclusion);
				});
	}

	private Map<String, SingleAllocBackTest> backTest(String code, String disp) {
		var sr = new Strategos();
		var ds = cfg.dataSource(code, period);

		return Read //
				.<String, BuySellStrategy> empty2() //
				.cons("longHold", sr.longHold) //
				.cons("lowPassPrediction", sr.lowPassPrediction(128, 8, 8, .02f)) //
				.cons("movingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f)) //
				.cons("macdSignalLineX", sr.macdSignalLineX(.8f, .9f, .85f)) //
				.cons("macdZeroLineX", sr.macdZeroLineX(.8f, .9f)) //
				.map2((sn, strategy) -> backTest_(ds, disp + ", strategy = " + sn, strategy)) //
				.toMap();
	}

	private SingleAllocBackTest backTest_(DataSource ds, String disp, BuySellStrategy strategy) {
		var backTest = SingleAllocBackTest.test(ds, strategy);
		LogUtil.info(disp + backTest.concludeLog);
		return backTest;
	}

}
