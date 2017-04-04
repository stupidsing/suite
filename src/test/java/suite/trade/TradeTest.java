package suite.trade;

import java.time.LocalDate;

import org.junit.Test;

import suite.os.LogUtil;
import suite.trade.Hkex.Company;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void testBackTest() {
		for (Company stock : new Hkex().hkex) {
			// String stockCode = "0066.HK"; // "JPY%3DX";
			String stockCode = stock.code + ".HK";
			String stockName = stock.name;
			String disp = stockCode + " " + stockName;
			try {
				backTest_(stockCode, disp);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + disp);
			}
		}
	}

	@Test
	public void testBackTestHkex0004() {
		backTest("0004.HK", "-");
	}

	@Test
	public void testBackTestHkex0005() {
		backTest("0005.HK", "HSBC"); // "JPY%3DX";
	}

	@Test
	public void testBackTestJpy() {
		backTest("JPY%3DX", "JPY");
	}

	private void backTest(String stockCode, String stockName) {
		backTest_(stockCode, stockCode + " " + stockName);

	}

	private void backTest_(String stockCode, String disp) {
		Strategos sr = new Strategos();
		DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
		backTest_(source, disp + ", strategy = lowPassPrediction", sr.lowPassPrediction(128, 8, 8, .02f));
		backTest_(source, disp + ", strategy = longHold", sr.longHold);
		backTest_(source, disp + ", strategy = movingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f));
		backTest_(source, disp + ", strategy = macdSignalLineX", sr.movingAvgConvDivSignalLineCrossover(.8f, .9f, .85f));
		backTest_(source, disp + ", strategy = macdZeroLineX", sr.movingAvgConvDivZeroCrossover(.8f, .9f));
	}

	private void backTest_(DataSource source, String disp, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info(disp //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
	}

}
