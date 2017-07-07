package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.FltFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account.Valuation;
import suite.util.Set_;
import suite.util.String_;
import suite.util.To;

public class Trade_ {

	public static Set<String> blackList = To.set("N/A"); // "0566.HK"
	public static boolean isCacheQuotes = true;
	public static boolean isShortSell = false;
	public static float leverageAmount = 100000f;
	public static float max = 1E6f;
	public static float negligible = 1E-6f;
	public static int nTradeDaysPerYear = 256;
	public static int nTradeSecondsPerDay = 28800;
	public static int thisYear = Time.now().year();

	public static double invTradeDaysPerYear = 1d / nTradeDaysPerYear;

	public static double riskFreeInterestRate = .013d; // .04d;
	public static double logRiskFreeInterestRate = Math.log1p(riskFreeInterestRate);

	public static double riskFreeInterestRate(int nDays) {
		return Math.expm1(logRiskFreeInterestRate * invTradeDaysPerYear * nDays);
	}

	public static List<Trade> diff(Map<String, Integer> assets0, Map<String, Integer> assets1, Obj_Flt<String> priceFun) {
		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());

		return Read.from(symbols) //
				.map2(symbol -> {
					int n0 = assets0.computeIfAbsent(symbol, s -> 0);
					int n1 = assets1.computeIfAbsent(symbol, s -> 0);
					return n1 - n0;
				}) //
				.filter((symbol, buySell) -> !String_.equals(symbol, Asset.cashSymbol)) //
				.map((symbol, buySell) -> Trade.of(buySell, symbol, priceFun.apply(symbol))) //
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
				.groupBy(sizes -> sizes.collectAsInt(Obj_Int.sum(size -> size))) //
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

	public static UpdatePortfolio updatePortfolio( //
			Account account, //
			List<Pair<String, Double>> ratioBySymbol, //
			Map<String, Asset> assetBySymbol, //
			Map<String, FltFltPair> priceBySymbol) {
		return new UpdatePortfolio(account, ratioBySymbol, assetBySymbol, priceBySymbol);
	}

	public static class UpdatePortfolio {
		public final Valuation val0;
		public final float valuation0;
		public final List<Trade> trades;

		public UpdatePortfolio( //
				Account account, //
				List<Pair<String, Double>> ratioBySymbol, //
				Map<String, Asset> assetBySymbol, //
				Map<String, FltFltPair> priceBySymbol) {
			Valuation val = account.valuation(symbol -> priceBySymbol.get(symbol).t0);
			float valuation = val.sum();

			Map<String, Integer> portfolio = Read //
					.from2(ratioBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
					.map2((symbol, potential) -> {
						float price = priceBySymbol.get(symbol).t0;
						int lotSize = assetBySymbol.get(symbol).lotSize;
						if (negligible < price)
							return lotSize * (int) Math.floor(valuation * potential / (price * lotSize));
						else
							return 0; // cannot buy liquidated stock
					}) //
					.toMap();

			List<Trade> trades_ = Trade_.diff(account.assets(), portfolio, symbol -> priceBySymbol.get(symbol).t1);

			val0 = val;
			valuation0 = valuation;
			trades = trades_;
		}
	}

}
