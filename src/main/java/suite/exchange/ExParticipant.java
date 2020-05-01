package suite.exchange;

import primal.MoreVerbs.Read;
import primal.primitive.FltPrim.Obj_Flt;
import primal.primitive.fp.AsDbl;
import suite.trade.Trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExParticipant {

	private Map<String, LimitOrderBook<String>.Order> orderByOrderId = new HashMap<>();
	private Map<String, ExPosition> positionByPositionId = new HashMap<>();
	private List<Trade> trades = new ArrayList<>();
	private float balance;

	public void deposit(float amount) {
		balance += amount;
	}

	public synchronized boolean record(int buySell, String symbolPositionId, float price, boolean isLeveraged) {
		return Exchange.sp(symbolPositionId).map((symbol, positionId) -> {
			var trade = Trade.of(buySell, symbol, price);

			var position = positionByPositionId.computeIfAbsent(symbolPositionId, p -> new ExPosition());

			position.adjust(buySell);

			if (!isLeveraged)
				balance -= buySell * price;
			else
				balance += position.enqueueFifo(trade);

			if (position.getBuySell() == 0)
				positionByPositionId.remove(symbolPositionId);

			return trades.add(trade);
		});
	}

	public synchronized void putOrder(String orderId, LimitOrderBook<String>.Order order) {
		orderByOrderId.put(orderId, order);
	}

	public synchronized LimitOrderBook<String>.Order removeOrder(String orderId) {
		return orderByOrderId.remove(orderId);
	}

	public synchronized ExPosition getPosition(String symbolPositionId) {
		return positionByPositionId.get(symbolPositionId);
	}

	public synchronized double margin(Obj_Flt<String> getCurrentPrice, double invLeverage) {
		var summary = summary_(getCurrentPrice, invLeverage);
		var nav = balance + summary.unrealizedPnl;
		return summary.marginUsed / nav;
	}

	public synchronized ExSummary summary(Obj_Flt<String> getCurrentPrice, double invLeverage) {
		return summary_(getCurrentPrice, invLeverage);
	}

	private ExSummary summary_(Obj_Flt<String> getCurrentPrice, double invLeverage) {
		var summaries = Read
				.from2(positionByPositionId)
				.map((symbolPositionId, position) -> Exchange.sp(symbolPositionId).map((symbol, positionId) -> position
						.summary(getCurrentPrice.apply(symbol), invLeverage)));

		var summary = new ExSummary();
		summary.vwapEntryPrice = Double.NaN;
		summary.unrealizedPnl = Read.from(summaries).toDouble(AsDbl.sum(s -> s.unrealizedPnl));
		summary.investedAmount = Read.from(summaries).toDouble(AsDbl.sum(s -> s.investedAmount));
		summary.marginUsed = Read.from(summaries).toDouble(AsDbl.sum(s -> s.marginUsed));
		return summary;
	}

}
