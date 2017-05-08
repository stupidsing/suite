package suite.trade.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.util.FunUtil.Fun;

public class QuoteTest {

	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();

	private Consumer<String> silent = s -> {
	};

	@Test
	public void testQuote() {
		System.out.println(yahoo.quote(new HashSet<>(Arrays.asList( //
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
				"2018.HK"))));
	}

	@Test
	public void testQuotes() {
		System.out.println("P/L = " + summarize(r -> "HKEX", silent));
	}

	@Test
	public void testQuotesDetail() {
		summarize(r -> "HKEX");
	}

	@Test
	public void testQuotesByStock() {
		System.out.println(summarize(r -> r.stockCode));
	}

	@Test
	public void testQuotesByStrategies() {
		System.out.println(summarize(r -> r.strategy, silent));
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun) {
		return summarize(fun, System.out::println);
	}

	private Map<String, Double> summarize(Fun<Trade, String> fun, Consumer<String> log) {
		List<Trade> table0 = TradeUtil.fromHistory(trade -> true);
		Map<String, Integer> nSharesByStockCodes = TradeUtil.portfolio(table0);
		Map<String, Float> priceByStockCodes = yahoo.quote(nSharesByStockCodes.keySet());
		int nTransactions = table0.size();
		double transactionAmount = Read.from(table0).collect(As.sumOfDoubles(trade -> trade.price * Math.abs(trade.buySell)));

		List<Trade> sellAll = Read.from(table0) //
				.groupBy(trade -> trade.strategy, st -> TradeUtil.portfolio(st.toList())) //
				.concatMap((strategy, nSharesByStockCode) -> Read //
						.from2(nSharesByStockCode) //
						.map((stockCode, size) -> {
							float price = priceByStockCodes.get(stockCode);
							return new Trade("-", -size, stockCode, price, strategy);
						})) //
				.toList();

		List<Trade> table1 = Streamlet.concat(Read.from(table0), Read.from(sellAll)).toList();

		double amount0 = TradeUtil.returns(table0);
		double amount1 = TradeUtil.returns(table1);

		Streamlet<String> constituents = Read.from2(nSharesByStockCodes) //
				.map((stockCode, nShares) -> {
					Asset asset = hkex.getCompany(stockCode);
					String shortName = asset != null ? asset.shortName() : null;
					float price = priceByStockCodes.get(stockCode);
					return stockCode + " (" + shortName + "): " + price + " * " + nShares + " = " + nShares * price;
				});

		log.accept("CONSTITUENTS:");
		constituents.forEach(log);
		log.accept("OWN = " + -amount0);
		log.accept("P/L = " + amount1);
		log.accept("nTransactions = " + nTransactions);
		log.accept("transactionAmount = " + transactionAmount);

		return Read.from(table1) //
				.groupBy(fun, st -> TradeUtil.returns(st.toList())) //
				.toMap();
	}

}
