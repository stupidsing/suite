package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;
import suite.util.TempDir;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	private Hkex hkex = new Hkex();

	@Test
	public void testBackTest() {
		for (Company stock : hkex.companies) {
			// String stockCode = "0066.HK"; // "JPY%3DX";
			String stockCode = stock.code + ".HK";
			String disp = stock.toString();
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
		for (String code : Arrays.asList( //
				"0002", //
				"0004", //
				"0005", //
				"0045", //
				"0066", //
				"0083", //
				"0175", //
				"0267", //
				"0293", //
				"1169", //
				"1357", //
				"2018"))
			backTest(code + ".HK", code);
	}

	@Test
	public void testBackTestHkexDetails() {
		backTest(hkex.getCompany("0066"));
	}

	@Test
	public void testQuote() {
		Yahoo yahoo = new Yahoo();
		List<String[]> table = Read.bytes(TempDir.resolve("stock.txt")) //
				.collect(As::table) //
				.toList();
		Map<String, Integer> sizeByStockCodes = Read.from(table) //
				.map2(array -> array[2], array -> Integer.parseInt(array[1])) //
				.groupBy(sizes -> sizes.fold(0, (size0, size1) -> size0 + size1)) //
				.toMap();
		float amount0 = Read.from(table) //
				.map(array -> Integer.parseInt(array[1]) * Float.parseFloat(array[3])) //
				.fold(0f, (amt0, amt1) -> amt0 + amt1);
		Map<String, Float> priceByStockCodes = yahoo.quote(Read.from(sizeByStockCodes.keySet()));
		float amount1 = Read.from2(sizeByStockCodes) //
				.map((stockCode, size) -> priceByStockCodes.get(stockCode) * size) //
				.fold(0f, (amt0, amt1) -> amt0 + amt1);
		System.out.println("AMOUNT0 = " + amount0);
		System.out.println("AMOUNT1 = " + amount1);
	}

	private void backTest(Company company) {
		backTest(company.code + ".HK", company.toString()) //
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
		DataSource ds = yahoo.dataSource(code, frDate, toDate);

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
