package suite.trade.singlealloc;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Forex;
import suite.trade.data.DataSource;
import suite.trade.data.Hkex;
import suite.trade.data.Yahoo;

public class BackTestTest {

	private DatePeriod period = DatePeriod.fiveYears();

	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void testBackTest() {
		for (Asset stock : hkex.queryCompanies()) {
			String disp = stock.toString();
			try {
				backTest(stock.code, disp);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + disp);
			}
		}
	}

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
		backTest(hkex.getCompany("0005.HK"));
	}

	private void backTest(Asset asset) {
		backTest(asset.code, asset.toString()) //
				.forEach((sn, backTest) -> {
					String conclusion = backTest.concludeLog.toString();
					LogUtil.info("BEGIN strategy = " + sn + conclusion);
					LogUtil.info(backTest.tradeLog.toString());
					LogUtil.info("END__ strategy = " + sn + conclusion);
				});
	}

	private Map<String, BackTest> backTest(String code, String disp) {
		Strategos sr = new Strategos();
		DataSource ds = yahoo.dataSource(code, period);

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

	private BackTest backTest_(DataSource ds, String disp, BuySellStrategy strategy) {
		BackTest backTest = BackTest.test(ds, strategy);
		LogUtil.info(disp + backTest.concludeLog);
		return backTest;
	}

}
