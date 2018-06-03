package suite.trade;

import static suite.util.Friends.abs;
import static suite.util.Friends.expm1;
import static suite.util.Friends.log1p;
import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.Math_;
import suite.primitive.Dbl_Dbl;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.IntIntSink;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntFltPair;
import suite.primitive.adt.pair.LngFltPair;
import suite.primitive.adt.pair.LngIntPair;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account.Valuation;
import suite.trade.data.DataSource.Eod;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Set_;
import suite.util.String_;

public class Trade_ {

	public static double barrier = 1d;
	public static Set<String> blackList = Set.of("0805.HK");
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
	public static double logRiskFreeInterestRate = log1p(riskFreeInterestRate);

	public static double riskFreeInterestRate(int nDays) {
		return expm1(logRiskFreeInterestRate * invTradeDaysPerYear * nDays);
	}

	public static Map<String, Float> collectAcquiredPrices(Outlet<Trade> outlet) {
		var acquireBySymbol = new HashMap<String, List<IntFltPair>>();

		for (var trade : outlet) {
			var symbol = trade.symbol;
			var buySell = trade.buySell;
			var price = trade.price;
			List<IntFltPair> acquires0 = acquireBySymbol.getOrDefault(symbol, List.of());
			var acquires1 = new ArrayList<IntFltPair>();

			for (var acquire : acquires0) {
				var n0 = acquire.t0;
				int diff = buySell < 0 ? max(0, min(-buySell, n0)) : min(0, max(-buySell, n0));
				var n1 = n0 - diff;
				buySell += diff;
				if (n1 != 0)
					acquires1.add(IntFltPair.of(n1, acquire.t1));
			}

			if (buySell != 0)
				acquires1.add(IntFltPair.of(buySell, price));

			acquireBySymbol.put(symbol, acquires1);
		}

		return Read //
				.from2(acquireBySymbol) //
				.mapValue(acquires -> {
					IntFltPair sum = IntFltPair.of(0, 0f);
					for (var acquire : acquires)
						sum.update(sum.t0 + acquire.t0, sum.t1 + acquire.t0 * acquire.t1);
					return sum.t1 / sum.t0;
				}) //
				.toMap();
	}

	public static Streamlet<Trade> collectBrokeredTrades(Outlet<Trade> outlet) {
		var trades0 = outlet.toArray(Trade.class);
		var trades1 = new ArrayList<Trade>();
		var length0 = trades0.length;
		var i0 = 0;

		IntIntSink tx = (i0_, i1_) -> {
			if (Ints_.range(i0_, i1_).mapInt(i -> trades0[i].buySell).sum() != 0)
				while (i0_ < i1_) {
					var trade0 = trades0[i0_++];
					if (!String_.equals(trade0.remark, "#"))
						trades1.add(trade0);
				}
		};

		for (var i = 1; i < length0; i++) {
			var trade0 = trades0[i0];
			var trade1 = trades0[i];
			var isGroup = true //
					&& String_.equals(trade0.date, trade1.date) //
					&& String_.equals(trade0.symbol, trade1.symbol) //
					&& trade0.price == trade1.price;
			if (!isGroup) {
				tx.sink2(i0, i);
				i0 = i;
			}
		}

		tx.sink2(i0, length0);
		return Read.from(trades1);
	}

	public static Streamlet<Trade> diff( //
			String time, //
			Map<String, Integer> assets0, //
			Map<String, Integer> assets1, //
			Obj_Flt<String> priceFun) {
		return diff_(time, assets0, assets1, priceFun);
	}

	public static float dividend(Streamlet<Trade> trades, Fun<String, LngFltPair[]> fun, Dbl_Dbl feeFun) {
		var sum = 0f;

		for (var pair : trades.toMultimap(trade -> trade.symbol).listEntries()) {
			var dividends = fun.apply(pair.t0);
			var outlet = Outlet.of(pair.t1);
			LngIntPair tn = LngIntPair.of(0l, 0);

			Source<LngIntPair> tradeSource = () -> {
				var trade = outlet.next();
				var t = trade != null ? Time.of(trade.date + " 12:00:00").epochSec(8) : Long.MAX_VALUE;
				return LngIntPair.of(t, tn.t1 + (trade != null ? trade.buySell : 0));
			};

			var tn1 = tradeSource.source();

			for (var dividend : dividends) {
				while (tn1 != null && tn1.t0 < dividend.t0) {
					tn.update(tn1.t0, tn1.t1);
					tn1 = tradeSource.source();
				}

				var amount = tn.t1 * dividend.t1;
				sum += amount - feeFun.apply(amount);
			}
		}

		return sum;
	}

	public static String format(Map<String, Integer> portfolio) {
		return Read //
				.from2(portfolio) //
				.sortBy((code, i) -> !String_.equals(code, Asset.cashSymbol) ? code : "") //
				.map((code, i) -> Math_.posNeg(i) + code + "*" + abs(i)) //
				.collect(As::joined);
	}

	public static String format(List<Trade> trades) {
		return Read //
				.from(trades) //
				.filter(trade -> trade.buySell != 0) //
				.collect(As::joined);
	}

	public static Map<String, Integer> portfolio(Iterable<Trade> trades) {
		return Read //
				.from(trades) //
				.map2(r -> r.symbol, r -> r.buySell) //
				.groupBy(sizes -> sizes.toInt(Obj_Int.sum(size -> size))) //
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
			var valuation = val.sum();

			var portfolio = Read //
					.from2(ratioBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
					.map2((symbol, potential) -> {
						var price = eodBySymbol.get(symbol).price;
						var lotSize = assetBySymbol.get(symbol).lotSize;
						return lotSize * (int) Math.floor(valuation * potential / (price * lotSize));
					}) //
					.toMap();

			Obj_Flt<String> priceFun;
			if (isMarketOrder)
				priceFun = symbol -> eodBySymbol.get(symbol).nextOpen;
			else
				priceFun = symbol -> eodBySymbol.get(symbol).price;

			var trades_ = Trade_ //
					.diff(time, account.assets(), portfolio, priceFun) //
					.partition(trade -> { // can be executed in next open price?
						var eod = eodBySymbol.get(trade.symbol);
						var price = trade.price;
						var priceBuy = price / barrier;
						var priceSell = price * barrier;
						var buySell = trade.buySell;

						// cannot buy liquidated stock
						var isTradeable = negligible < price;

						// only if trade is within price range of next tick
						var isMatch = isFreePlay //
								|| 0 < buySell && eod.nextLow <= priceBuy //
								|| buySell < 0 && priceSell <= eod.nextHigh;

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
				.collect();
	}

}
