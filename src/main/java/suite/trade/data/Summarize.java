package suite.trade.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Summarize {

	private Fun<Set<String>, Map<String, Float>> quoteFun;
	private Fun<String, Asset> getAssetFun;

	private Hsbc broker = new Hsbc();

	public Summarize(Fun<Set<String>, Map<String, Float>> quoteFun, Fun<String, Asset> getAssetFun) {
		this.quoteFun = quoteFun;
		this.getAssetFun = getAssetFun;
	}

	public Map<String, Double> summarize(Fun<Trade, String> fun, Consumer<String> log) {
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
		log.accept("transactionFee = " + To.string(broker.transactionFee(transactionAmount)));

		return Read.from(table1) //
				.groupBy(fun, st -> TradeUtil.returns(st.toList())) //
				.toMap();
	}

}
