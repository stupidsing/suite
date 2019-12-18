package suite.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Get;
import primal.adt.FixieArray;
import suite.exchange.LimitOrderBook.LobListener;
import suite.trade.Trade;

public class Exchange {

	private boolean isLeveraged = true;
	private int leverage = 100;
	private double invLeverage = 1d / leverage;

	private Map<String, Participant> participantById = new ConcurrentHashMap<>();
	private Map<String, LimitOrderBook<String>> lobBySymbol = new ConcurrentHashMap<>();
	private Map<String, MarketData> marketDataBySymbol = new ConcurrentHashMap<>();

	private class Participant {
		private Map<String, LimitOrderBook<String>.Order> orderByOrderId = new HashMap<>();
		private Map<String, Position> positionByPositionId = new HashMap<>();
		private List<Trade> trades = new ArrayList<>();
		private float balance;
		private float marginUsed;

		private synchronized boolean record(int buySell, String symbolPositionId, float price) {
			return sp(symbolPositionId).map((symbol, positionId) -> {
				var position = positionByPositionId.get(positionId);

				if (!isLeveraged)
					balance -= buySell * price;
				else
					marginUsed += Math.abs(buySell) * price * invLeverage;

				position.buySell += buySell;

				if (position.buySell == 0)
					positionByPositionId.remove(symbolPositionId);

				return trades.add(Trade.of(buySell, symbol, price));
			});
		}

		private synchronized void put( //
				String orderId, LimitOrderBook<String>.Order order, //
				String positionId, Position position) {
			orderByOrderId.put(orderId, order);
			positionByPositionId.put(positionId, position);
		}

		private synchronized LimitOrderBook<String>.Order remove(String orderId) {
			return orderByOrderId.remove(orderId);
		}
	}

	private class Position {
		private int buySell;
	}

	public String orderNew(String participantId, int buySell, String symbol, float price) {
		var positionId = !isLeveraged ? "" : "P" + Get.temp();
		return submitOrder(participantId, positionId, buySell, symbol, price);
	}

	public String positionClose(String participantId, String symbolPositionId, float price) {
		var buySell = sp(symbolPositionId).map((symbol, positionId) -> {
			return participantById.get(participantId).positionByPositionId.get(positionId).buySell;
		});
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
		participantById.get(participantId).put(orderId, order, positionId, new Position());
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
						.remove(orderId));
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
