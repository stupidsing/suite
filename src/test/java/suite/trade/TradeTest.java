package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	private Hkex hkex = new Hkex();

	@Test
	public void testBackTest() {
		for (Company stock : hkex.companies) {
			// String stockCode = "0066.HK"; // "JPY%3DX";
			String stockCode = stock.code + ".HK";
			String stockName = stock.name;
			String disp = stockCode + " " + stockName;
			try {
				backTest(stockCode, disp);
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
		for (String code : Arrays.asList("0004", "0005"))
			backTest(code + ".HK", code);
	}

	@Test
	public void testBackTestHkex0004() {
		backTest(hkex.getCompany("0004")); // Wharf (Holdings) Ltd., The
	}

	@Test
	public void testBackTestHkex0005() {
		backTest(hkex.getCompany("0005")); // HSBC Holdings Plc
	}

	private void backTest(Company company) {
		String disp = company.code + " " + company.name;
		backTest(company.code + ".HK", disp) //
				.forEach((sn, backTest) -> {
					LogUtil.info("strategy = " + sn);
					LogUtil.info(backTest.log.toString());
				});
	}

	private Map<String, BackTest> backTest(String code, String disp) {
		Strategos sr = new Strategos();
		DataSource ds = DataSource.yahoo(code, frDate, toDate);

		return Read //
				.<String, Strategy> empty2() //
				.cons("longHold", sr.longHold) //
				.cons("lowPassPrediction", sr.lowPassPrediction(128, 8, 8, .02f)) //
				.cons("movingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f)) //
				.cons("macdSignalLineX", sr.macdSignalLineX(.8f, .9f, .85f)) //
				.cons("macdZeroLineX", sr.macdZeroLineX(.8f, .9f)) //
				.mapEntry((sn, strategy) -> sn, (sn, strategy) -> backTest_(ds, disp + ", strategy = " + sn, strategy)) //
				.toMap();
	}

	private BackTest backTest_(DataSource ds, String disp, Strategy strategy) {
		BackTest backTest = BackTest.test(ds, strategy);
		Account account = backTest.account;
		LogUtil.info(disp //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
		return backTest;
	}

}
