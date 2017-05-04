package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Test;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trans.Record;
import suite.util.Util;

public class QuoteTest {

	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void testQuote() {
		System.out.println(yahoo.quote(Read.each( //
				"0002.HK", //
				"0004.HK", //
				"0005.HK", //
				"0045.HK", //
				"0066.HK", //
				"0083.HK", //
				"0175.HK", //
				"0267.HK", //
				"0293.HK", //
				"0322.HK", //
				"1169.HK", //
				"1357.HK", //
				"2018.HK")));
	}

	@Test
	public void testQuotes() {
		summarize(r -> true);
	}

	@Test
	public void testQuotesByStock() {
		summarize(r -> Util.stringEquals(r.stockCode, "0005.HK"));
	}

	@Test
	public void testQuotesByStrategies() {
		Map<String, Double> profitAndLossByStrategy = Read.each("mamr", "manual", "pmamr") //
				.map2(strategy -> summarize(r -> r.strategy.startsWith(strategy))) //
				.toMap();

		System.out.println(profitAndLossByStrategy);
	}

	private double summarize(Predicate<Record> pred) {
		List<Record> table0 = Trans.fromHistory(pred);
		Map<String, Integer> nSharesByStockCodes = Trans.portfolio(table0);
		Map<String, Float> priceByStockCodes = yahoo.quote(Read.from(nSharesByStockCodes.keySet()));

		List<Record> sellAll = Read.from2(nSharesByStockCodes) //
				.map((stockCode, size) -> {
					float price = priceByStockCodes.get(stockCode);
					return new Record("-", -size, stockCode, price, "-");
				}) //
				.toList();

		List<Record> table1 = Streamlet.concat(Read.from(table0), Read.from(sellAll)).toList();

		double amount0 = Trans.returns(table0);
		double amount1 = Trans.returns(table1);

		Streamlet<String> constituents = Read.from2(nSharesByStockCodes) //
				.map((stockCode, nShares) -> {
					Asset asset = hkex.getCompany(stockCode);
					String shortName = asset != null ? asset.shortName() : null;
					float price = priceByStockCodes.get(stockCode);
					return stockCode + " (" + shortName + ") := " + price + " * " + nShares + " == " + nShares * price;
				});

		System.out.println("CONSTITUENTS:");
		constituents.forEach(System.out::println);
		System.out.println("OWN = " + amount0);
		System.out.println("P/L = " + amount1);

		return amount1;
	}

}
