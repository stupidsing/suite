package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.Util;

public class TradeUtil {

	public static Map<String, Double> summarize( //
			Fun<Trade, String> fun, //
			Fun<Set<String>, Map<String, Float>> quoteFun, //
			Fun<String, Asset> getAssetFun, //
			Consumer<String> log) {
		List<Trade> table0 = TradeUtil.fromHistory(trade -> true);
		Map<String, Integer> nSharesByStockCodes = TradeUtil.portfolio(table0);
		Set<String> stockCodes = nSharesByStockCodes.keySet();
		Map<String, Float> priceByStockCodes = quoteFun.apply(stockCodes);
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
					Asset asset = getAssetFun.apply(stockCode);
					float price = priceByStockCodes.get(stockCode);
					return asset + ": " + price + " * " + nShares + " = " + nShares * price;
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

	public static String format(Map<String, Integer> portfolio) {
		return Read.from2(portfolio) //
				.sortBy((code, i) -> !Util.stringEquals(code, Asset.cash.code) ? code : "") //
				.map((code, i) -> code + ":" + i + ",") //
				.collect(As.joined());
	}

	public static List<Trade> fromHistory(Predicate<Trade> pred) {
		return memoizeHistoryRecords.source().filter(pred).toList();
	}

	private static Source<Streamlet<Trade>> memoizeHistoryRecords = Memoize.source(TradeUtil::historyRecords);

	private static Streamlet<Trade> historyRecords() {
		return Read.url("https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt") //
				.collect(As::table) //
				.map(Trade::new) //
				.collect(As::streamlet);
	}

	public static Map<String, Integer> portfolio(List<Trade> trades) {
		return Read.from(trades) //
				.map2(r -> r.stockCode, r -> r.buySell) //
				.groupBy(sizes -> sizes.collect(As.sumOfInts(size -> size))) //
				.filterValue(size -> size != 0) //
				.toMap();
	}

	// Profit & loss
	public static double returns(List<Trade> trades) {
		return Read.from(trades).collect(As.sumOfDoubles(r -> -r.buySell * r.price));
	}

	public static List<Pair<String, Integer>> diff(Map<String, Integer> assets0, Map<String, Integer> assets1) {
		Set<String> stockCodes = Streamlet2.concat(Read.from2(assets0), Read.from2(assets1)) //
				.map((stockCode, nShares) -> stockCode) //
				.toSet();

		return Read.from(stockCodes) //
				.map2(stockCode -> {
					int n0 = assets0.computeIfAbsent(stockCode, s -> 0);
					int n1 = assets1.computeIfAbsent(stockCode, s -> 0);
					return n1 - n0;
				}) //
				.filter((stockCode, n) -> !Util.stringEquals(stockCode, Asset.cash.code)) //
				.toList();
	}

}
