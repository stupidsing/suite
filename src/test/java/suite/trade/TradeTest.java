package suite.trade;

import java.time.LocalDate;

import org.junit.Test;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.os.LogUtil;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void testBackTest() {
		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex) {
			// String stockCode = "0066.HK"; // "JPY%3DX";
			String stockCode = stock.t0 + ".HK";
			String stockName = stock.t1;
			String disp = stockCode + " " + stockName;
			try {
				backTest_(stockCode, disp);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + disp);
			}
		}
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
		backTest_(source, disp + ", strategy = LongHold", sr.longHold);
		backTest_(source, disp + ", strategy = MovingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, .15f));
		backTest_(source, disp + ", strategy = macdSignalLineX", sr.movingAvgConvDivSignalLineCrossover(.8f, .9f, .85f));
		backTest_(source, disp + ", strategy = macdZeroLineX", sr.movingAvgConvDivZeroCrossover(.8f, .9f));
	}

	private void backTest_(DataSource source, String prefix, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info(prefix //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
	}

}
