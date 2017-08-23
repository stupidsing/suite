package suite.trade;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.MathUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account.Valuation;
import suite.trade.data.DataSource.Eod;
import suite.util.Set_;
import suite.util.String_;

public class Trade_ {

	public static Set<String> blackList = Collections.emptySet();
	public static boolean isCacheQuotes = true;
	public static boolean isFreePlay = false;
	public static boolean isMarketOrder = true;
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

	public static Streamlet<Trade> diff( //
			String time, //
			Map<String, Integer> assets0, //
			Map<String, Integer> assets1, //
			Obj_Flt<String> priceFun) {
		return diff_(time, assets0, assets1, priceFun);
	}

	public static String format(Map<String, Integer> portfolio) {
		return Read.from2(portfolio) //
				.sortBy((code, i) -> !String_.equals(code, Asset.cashSymbol) ? code : "") //
				.map((code, i) -> MathUtil.posNeg(i) + code + "*" + Math.abs(i)) //
				.collect(As::joined);
	}

	public static String format(List<Trade> trades) {
		return Read.from(trades) //
				.filter(trade -> trade.buySell != 0) //
				.map(Trade::toString) //
				.collect(As::joined);
	}

	public static Map<String, Integer> portfolio(Iterable<Trade> trades) {
		return Read.from(trades) //
				.map2(r -> r.symbol, r -> r.buySell) //
				.groupBy(sizes -> sizes.collectAsInt(Obj_Int.sum(size -> size))) //
				.filterValue(size -> size != 0) //
				.toMap();
	}

	public static Streamlet<Trade> sellAll(String time, Streamlet<Trade> trades, Obj_Flt<String> priceFun) {
		return trades //
				.groupBy(trade -> trade.strategy, Trade_::portfolio) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> Trade.of(time, -size, symbol, priceFun.apply(symbol), strategy)));
	}

	public static UpdatePortfolio updatePortfolio( //
			String time, //
			Account account, //
			List<Pair<String, Double>> ratioBySymbol, //
			Map<String, Asset> assetBySymbol, //
			Map<String, Eod> eodBySymbol) {
		return new UpdatePortfolio(time, account, ratioBySymbol, assetBySymbol, eodBySymbol);
	}

	public static class UpdatePortfolio {
		public final Valuation val0;
		public final float valuation0;
		public final List<Trade> trades;

		private UpdatePortfolio( //
				String time, //
				Account account, //
				List<Pair<String, Double>> ratioBySymbol, //
				Map<String, Asset> assetBySymbol, //
				Map<String, Eod> eodBySymbol) {
			Valuation val = account.valuation(symbol -> eodBySymbol.get(symbol).price);
			float valuation = val.sum();

			Map<String, Integer> portfolio = Read //
					.from2(ratioBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
					.map2((symbol, potential) -> {
						float price = eodBySymbol.get(symbol).price;
						int lotSize = assetBySymbol.get(symbol).lotSize;
						return lotSize * (int) Math.floor(valuation * potential / (price * lotSize));
					}) //
					.toMap();

			Obj_Flt<String> priceFun;
			if (isMarketOrder)
				priceFun = symbol -> eodBySymbol.get(symbol).nextOpen;
			else
				priceFun = symbol -> eodBySymbol.get(symbol).price;

			List<Trade> trades_ = Trade_ //
					.diff(time, account.assets(), portfolio, priceFun) //
					.partition(trade -> { // can be executed in next open price?
						Eod eod = eodBySymbol.get(trade.symbol);
						float price = trade.price;
						int buySell = trade.buySell;

						// cannot buy liquidated stock
						boolean isTradeable = negligible < price;

						// only if trade is within price range of next tick
						boolean isMatch = isFreePlay //
								|| 0 < buySell && eod.nextLow <= price //
								|| buySell < 0 && price <= eod.nextHigh;

						return isTradeable && isMatch;
					}).t0 //
							.sortBy(trade -> trade.buySell) // sell first
							.toList();

			val0 = val;
			valuation0 = valuation;
			trades = trades_;
		}
	}

	public static boolean isValidCash(int cash) {
		return -Trade_.leverageAmount <= cash;
	}

	public static boolean isValidStock(String symbol, int nShares) {
		return Trade_.isShortSell || String_.equals(symbol, Asset.cashSymbol) || 0 <= nShares;
	}

	private static Streamlet<Trade> diff_( //
			String time, //
			Map<String, Integer> assets0, //
			Map<String, Integer> assets1, //
			Obj_Flt<String> priceFun) {
		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());

		return Read //
				.from(symbols) //
				.map2(symbol -> {
					int n0 = assets0.getOrDefault(symbol, 0);
					int n1 = assets1.getOrDefault(symbol, 0);
					return n1 - n0;
				}) //
				.filter((symbol, buySell) -> true //
						&& !String_.equals(symbol, Asset.cashSymbol) //
						&& buySell != 0) //
				.map((symbol, buySell) -> Trade.of(time, buySell, symbol, priceFun.apply(symbol), "-")) //
				.collect(As::streamlet);
	}

}
