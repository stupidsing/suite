package suite.trade;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;

public class TradeTest {

	private Period period = Period.fiveYears();

	private Hkex hkex = new Hkex();

	@Test
	public void testBackTest() {
		for (Company stock : hkex.companies) {
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
				"0020", //
				"0322", //
				"1128", //
				"1169", //
				"1357"))
			backTest(code + ".HK", code);
	}

	@Test
	public void testBackTestHkexDetails() {
		backTest(hkex.getCompany("0069.HK"));
	}

	private void backTest(Company company) {
		backTest(company.code, company.toString()) //
				.forEach((sn, backTest) -> {
					String conclusion = backTest.concludeLog.toString();
					LogUtil.info("BEGIN strategy = " + sn + conclusion);
					LogUtil.info(backTest.tradeLog.toString());
					LogUtil.info("END__ strategy = " + sn + conclusion);
				});
	}

	private Map<String, BackTest> backTest(String code, String disp) {
		Yahoo yahoo = new Yahoo();
		Strategos sr = new Strategos();
		DataSource ds = yahoo.dataSource(code, period);

		return Read //
				.<String, Strategy> empty2() //
				.cons("longHold", sr.longHold) //
				.cons("lowPassPrediction", sr.lowPassPrediction(128, 8, 8, .02f)) //
				.cons("movingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f)) //
				.cons("macdSignalLineX", sr.macdSignalLineX(.8f, .9f, .85f)) //
				.cons("macdZeroLineX", sr.macdZeroLineX(.8f, .9f)) //
				.map2((sn, strategy) -> sn, (sn, strategy) -> backTest_(ds, disp + ", strategy = " + sn, strategy)) //
				.toMap();
	}

	private BackTest backTest_(DataSource ds, String disp, Strategy strategy) {
		BackTest backTest = BackTest.test(ds, strategy);
		LogUtil.info(disp + backTest.concludeLog);
		return backTest;
	}

}
