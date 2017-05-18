package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.primitive.PrimitiveFun.Obj_Float;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.String_;

public class TradeUtil {

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
				.filter((symbol, buySell) -> !String_.equals(symbol, Asset.cashCode)) //
				.map((symbol, buySell) -> Trade.of(buySell, symbol, prices.get(symbol))) //
				.toList();
	}

	public static String format(Map<String, Integer> portfolio) {
		return Read.from2(portfolio) //
				.sortBy((code, i) -> !String_.equals(code, Asset.cashCode) ? code : "") //
				.map((code, i) -> "+" + code + "*" + i) //
				.collect(As.joined());
	}

	public static String format(List<Trade> trades) {
		return Read.from(trades) //
				.filter(trade -> trade.buySell != 0) //
				.map(Trade::toString) //
				.collect(As.joined());
	}

	public static Map<String, Integer> portfolio(Iterable<Trade> trades) {
		return Read.from(trades) //
				.map2(r -> r.symbol, r -> r.buySell) //
				.groupBy(sizes -> sizes.collectAsInt(As.sumOfInts(size -> size))) //
				.filterValue(size -> size != 0) //
				.toMap();
	}

	public static Streamlet<Trade> sellAll(Streamlet<Trade> trades, Obj_Float<String> priceFun) {
		return trades //
				.groupBy(trade -> trade.strategy, TradeUtil::portfolio) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> Trade.of(-size, symbol, priceFun.applyAsFloat(symbol), strategy)));
	}

}
