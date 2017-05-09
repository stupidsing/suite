package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.Util;

public class TradeUtil {

	public static String format(Map<String, Integer> portfolio) {
		return Read.from2(portfolio) //
				.sortBy((code, i) -> !Util.stringEquals(code, Asset.cashCode) ? code : "") //
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

	public static Map<String, Integer> portfolio(Iterable<Trade> trades) {
		return Read.from(trades) //
				.map2(r -> r.symbol, r -> r.buySell) //
				.groupBy(sizes -> sizes.collect(As.sumOfInts(size -> size))) //
				.filterValue(size -> size != 0) //
				.toMap();
	}

	public static List<Trade> diff(Map<String, Integer> assets0, Map<String, Integer> assets1, Map<String, Float> prices) {
		Set<String> symbols = Streamlet2.concat(Read.from2(assets0), Read.from2(assets1)) //
				.map((symbol, nShares) -> symbol) //
				.toSet();

		return Read.from(symbols) //
				.map2(symbol -> {
					int n0 = assets0.computeIfAbsent(symbol, s -> 0);
					int n1 = assets1.computeIfAbsent(symbol, s -> 0);
					return n1 - n0;
				}) //
				.filter((symbol, buySell) -> !Util.stringEquals(symbol, Asset.cashCode)) //
				.map((symbol, buySell) -> new Trade(buySell, symbol, prices.get(symbol))) //
				.toList();
	}

}
