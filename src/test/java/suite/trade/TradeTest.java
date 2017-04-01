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
				backTestEquity(stockCode, disp);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + disp);
			}
		}
	}

	@Test
	public void testBackTestHkex0005() {
		backTestEquity("0066.HK", "HSBC"); // "JPY%3DX";
	}

	@Test
	public void testBackTestJpy() {
		backTestEquity("JPY%3DX", "JPY");
	}

	private void backTestEquity(String stockCode, String disp) {
		Strategos sr = new Strategos();
		DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
		backTest(source, disp + ", strategy = lowPassPrediction", sr.lowPassPrediction(128, 8, 8, 0.02f));
		backTest(source, disp + ", strategy = LongHold", sr.longHold);
		backTest(source, disp + ", strategy = MovingAvgMeanReverting", sr.movingAvgMeanReverting(64, 8, 0.15f));
	}

	private void backTest(DataSource source, String prefix, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info(prefix //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
	}

}
