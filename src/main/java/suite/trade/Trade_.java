package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.primitive.FltFun.Obj_Flt;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Set_;
import suite.util.String_;

public class Trade_ {

	public static boolean isShortSell = false;
	public static float maxLeverageAmount = 100000f;
	public static int nTradeDaysPerYear = 256;

	public static double invTradeDaysPerYear = 1d / nTradeDaysPerYear;

	public static double riskFreeInterestRate = .013d; // .04d;
	public static double logRiskFreeInterestRate = Math.log1p(riskFreeInterestRate);

	public static double riskFreeInterestRate(int nDays) {
		return Math.expm1(logRiskFreeInterestRate * invTradeDaysPerYear * nDays);
	}

	public static List<Trade> diff(Map<String, Integer> assets0, Map<String, Integer> assets1, Map<String, Float> prices) {
		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());

		return Read.from(symbols) //
				.map2(symbol -> {
					int n0 = assets0.computeIfAbsent(symbol, s -> 0);
					int n1 = assets1.computeIfAbsent(symbol, s -> 0);
					return n1 - n0;
				}) //
				.filter((symbol, buySell) -> !String_.equals(symbol, Asset.cashSymbol)) //
				.map((symbol, buySell) -> Trade.of(buySell, symbol, prices.get(symbol))) //
				.toList();
	}

	public static String format(Map<String, Integer> portfolio) {
		return Read.from2(portfolio) //
				.sortBy((code, i) -> !String_.equals(code, Asset.cashSymbol) ? code : "") //
				.map((code, i) -> (0 <= i ? "+" : "-") + code + "*" + Math.abs(i)) //
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

	public static Streamlet<Trade> sellAll(Streamlet<Trade> trades, Obj_Flt<String> priceFun) {
		return trades //
				.groupBy(trade -> trade.strategy, Trade_::portfolio) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> Trade.of(-size, symbol, priceFun.apply(symbol), strategy)));
	}

}
