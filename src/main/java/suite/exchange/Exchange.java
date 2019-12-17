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

	private Map<String, Participant> participantById = new ConcurrentHashMap<>();
	private Map<String, LimitOrderBook<String>> lobBySymbol = new ConcurrentHashMap<>();
	private Map<String, MarketData> marketDataBySymbol = new ConcurrentHashMap<>();

	private class Participant {
		private Map<String, LimitOrderBook<String>.Order> orderById = new HashMap<>();
		private Map<String, Integer> positionBySymbol = new HashMap<>();
		private List<Trade> trades = new ArrayList<>();
		private float balance;

		private synchronized boolean record(int buySell, String symbol, float price) {
			balance -= buySell * price;

			positionBySymbol.compute(symbol, (s, balance0) -> {
				var balancex = (balance0 != null ? balance0 : 0) + buySell;
				return balancex != 0 ? balancex : null;
			});

			return trades.add(Trade.of(buySell, symbol, price));
		}

		private synchronized LimitOrderBook<String>.Order put(String orderId, LimitOrderBook<String>.Order order) {
			return orderById.put(orderId, order);
		}

		private synchronized LimitOrderBook<String>.Order remove(String orderId) {
			return orderById.remove(orderId);
		}
	}

	public synchronized void submit(String participantId, int buySell, String symbol, float price) {
		var orderId = "O#" + Get.temp();
		var lob = lob(symbol);

		var order = lob.new Order();
		order.id = participantId + ":" + symbol + ":" + orderId;
		order.isMarket = Float.isNaN(price);
		order.price = !order.isMarket ? price : 0 < buySell ? Float.MAX_VALUE : Float.MIN_VALUE;
		order.buySell = buySell;

		lob.update(null, order);
		participantById.get(participantId).put(orderId, order);
	}

	private LimitOrderBook<String> lob(String symbol) {
		var marketData = marketDataBySymbol.computeIfAbsent(symbol, s -> new MarketData());

		return lobBySymbol.computeIfAbsent(symbol, s -> new LimitOrderBook<>(new LobListener<>() {
			public void handleOrderFulfilled(LimitOrderBook<String>.Order order, float price, int buySell) {
				pso(order).map((participantId, symbol, orderId) -> participantById //
						.get(participantId) //
						.record(buySell, symbol, price));
			}

			public void handleOrderDisposed(LimitOrderBook<String>.Order order) {
				pso(order).map((participantId, symbol, orderId) -> participantById //
						.get(participantId) //
						.remove(orderId));
			}

			public void handleQuoteChange(float bid, float ask, int volume) {
				marketData.update(System.currentTimeMillis(), (bid + ask) * .5f, volume);
			}
		}));
	}

	private FixieArray<String> pso(LimitOrderBook<String>.Order order) {
		return FixieArray.of(order.id.split(":"));
	}

}
