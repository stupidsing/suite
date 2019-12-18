package suite.exchange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.adt.FixieArray;
import primal.primitive.fp.AsDbl;
import suite.exchange.LimitOrderBook.LobListener;
import suite.trade.Trade;

public class Exchange {

	private boolean isLeveraged = true;
	private int leverage = 100;
	private double invLeverage = 1d / leverage;

	private Map<String, ExParticipant> participantById = new ConcurrentHashMap<>();
	private Map<String, LimitOrderBook<String>> lobBySymbol = new ConcurrentHashMap<>();
	private Map<String, MarketData> marketDataBySymbol = new ConcurrentHashMap<>();

	private class ExParticipant {
		private Map<String, LimitOrderBook<String>.Order> orderByOrderId = new HashMap<>();
		private Map<String, ExPosition> positionByPositionId = new HashMap<>();
		private List<Trade> trades = new ArrayList<>();
		private float balance;

		private synchronized boolean record(int buySell, String symbolPositionId, float price) {
			return sp(symbolPositionId).map((symbol, positionId) -> {
				var trade = Trade.of(buySell, symbol, price);

				var position = positionByPositionId.computeIfAbsent(symbolPositionId, p -> new ExPosition());
				var buySell0 = position.buySell;
				var buySell1 = buySell0 + buySell;

				position.buySell = buySell1;

				if (!isLeveraged)
					balance -= buySell * price;
				else
					balance += position.enqueueFifo(trade);

				if (position.buySell == 0)
					positionByPositionId.remove(symbolPositionId);

				return trades.add(trade);
			});
		}

		private synchronized void putOrder(String orderId, LimitOrderBook<String>.Order order) {
			orderByOrderId.put(orderId, order);
		}

		private synchronized LimitOrderBook<String>.Order removeOrder(String orderId) {
			return orderByOrderId.remove(orderId);
		}

		private synchronized ExPosition getPosition(String symbolPositionId) {
			return positionByPositionId.get(symbolPositionId);
		}

		private synchronized Summary summary(Map<String, Float> currentPriceBySymbol, double invLeverage) {
			var summaries = Read //
					.from2(positionByPositionId) //
					.map((symbolPositionId, position) -> sp(symbolPositionId).map((symbol, positionId) -> position //
							.summary(currentPriceBySymbol.get(symbol), invLeverage)));

			var summary = new Summary();
			summary.unrealizedPnl = Read.from(summaries).toDouble(AsDbl.sum(s -> s.unrealizedPnl));
			summary.investedAmount = Read.from(summaries).toDouble(AsDbl.sum(s -> s.investedAmount));
			summary.marginUsed = Read.from(summaries).toDouble(AsDbl.sum(s -> s.marginUsed));
			return summary;
		}

		private class Summary {
			private double unrealizedPnl;
			private double investedAmount;
			private double marginUsed;
		}
	}

	private class ExPosition {
		private int buySell;
		private Deque<Fifo> fifos = new ArrayDeque<>();

		private class Fifo {
			private int buySell;
			private float entryPrice;
		}

		private synchronized double enqueueFifo(Trade trade) {
			Fifo last = new Fifo(); // outstanding
			last.buySell = trade.buySell;
			last.entryPrice = trade.price;

			var realizedPnl = 0f;
			Fifo first;

			while ((first = fifos.peekFirst()) != null && Math.signum(last.buySell) != Math.signum(first.buySell)) {
				int bsd;
				if (last.buySell < 0)
					bsd = Math.min(-last.buySell, first.buySell);
				else
					bsd = Math.max(-last.buySell, first.buySell);

				last.buySell += bsd;
				first.buySell -= bsd;
				realizedPnl += bsd * (last.entryPrice - first.entryPrice);

				if (first.buySell == 0)
					fifos.removeFirst();
			}

			if (last.buySell != 0)
				fifos.addLast(last);

			return realizedPnl;
		}

		private int getBuySell() {
			return buySell;
		}

		private synchronized Summary summary(float currentPrice, double invLeverage) {
			var n = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell * fifo.entryPrice));
			var d = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell));

			var summary = new Summary();
			summary.vwapEntryPrice = n / d;
			summary.unrealizedPnl = Read.from(fifos)
					.toDouble(AsDbl.sum(fifo -> fifo.buySell * (currentPrice - fifo.entryPrice)));
			summary.investedAmount = Read.from(fifos).toDouble(AsDbl.sum(fifo -> fifo.buySell * currentPrice));
			summary.marginUsed = summary.investedAmount * invLeverage;
			return summary;
		}

		private class Summary {
			private double vwapEntryPrice;
			private double unrealizedPnl;
			private double investedAmount;
			private double marginUsed;
		}
	}

	public String orderNew(String participantId, int buySell, String symbol, float price) {
		var positionId = !isLeveraged ? "" : "P" + Get.temp();
		return submitOrder(participantId, positionId, buySell, symbol, price);
	}

	public String positionClose(String participantId, String symbolPositionId, float price) {
		var buySell = participantById.get(participantId).getPosition(symbolPositionId).getBuySell();
		return positionClosePartially(participantId, symbolPositionId, -buySell, price);
	}

	public String positionClosePartially(String participantId, String symbolPositionId, int buySell, float price) {
		return sp(symbolPositionId).map((symbol, positionId) -> {
			return submitOrder(participantId, positionId, buySell, symbol, price);
		});
	}

	private String submitOrder(String participantId, String positionId, int buySell, String symbol, float price) {
		var orderId = "O" + Get.temp();
		var symbolPositionId = symbol + "#" + positionId;
		var lob = lob(symbol);

		var order = lob.new Order();
		order.id = participantId + ":" + symbolPositionId + ":" + orderId;
		order.isMarket = Float.isNaN(price);
		order.price = !order.isMarket ? price : 0 < buySell ? Float.MAX_VALUE : Float.MIN_VALUE;
		order.buySell = buySell;

		lob.update(null, order);
		participantById.get(participantId).putOrder(orderId, order);
		return symbolPositionId;
	}

	private LimitOrderBook<String> lob(String symbol) {
		var marketData = marketDataBySymbol.computeIfAbsent(symbol, s -> new MarketData());

		return lobBySymbol.computeIfAbsent(symbol, s -> new LimitOrderBook<>(new LobListener<>() {
			public void handleOrderFulfilled(LimitOrderBook<String>.Order order, float price, int buySell) {
				ppo(order).map((participantId, symbolPositionId, orderId) -> participantById //
						.get(participantId) //
						.record(buySell, symbolPositionId, price));
			}

			public void handleOrderDisposed(LimitOrderBook<String>.Order order) {
				ppo(order).map((participantId, symbolPositionId, orderId) -> participantById //
						.get(participantId) //
						.removeOrder(orderId));
			}

			public void handleQuoteChanged(float bid, float ask, int volume) {
				marketData.update(System.currentTimeMillis(), (bid + ask) * .5f, volume);
			}
		}));
	}

	private FixieArray<String> ppo(LimitOrderBook<String>.Order order) {
		return FixieArray.of(order.id.split(":"));
	}

	private FixieArray<String> sp(String symbolPositionId) {
		return FixieArray.of(symbolPositionId.split("#"));
	}

}
