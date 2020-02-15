package suite.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Get;
import primal.adt.FixieArray;
import primal.adt.Mutable;
import suite.exchange.LimitOrderBook.LobListener;

public class Exchange {

	private boolean isHedged = false;
	private boolean isLeveraged = true;
	private int leverage = !isLeveraged ? 1 : 100;
	private double invLeverage = 1d / leverage;

	private Map<String, ExParticipant> participantById = new ConcurrentHashMap<>();
	private Map<String, LimitOrderBook<String>> lobBySymbol = new ConcurrentHashMap<>();
	private Map<String, MarketData> marketDataBySymbol = new ConcurrentHashMap<>();

	public interface Participant {
		public ExSummary summary();

		public Order order(String symbol);
	}

	public interface Order {
		public void new_(int buySell, float price);

		public String amend(int buySell, float price);

		public void cancel();

		public int fulfilled();

		public Position position();
	}

	public interface Position {
		public Order close();

		public Order closePartially(int buySell);
	}

	public static FixieArray<String> sp(String symbolPositionId) {
		return FixieArray.of(symbolPositionId.split("#"));
	}

	public Participant newParticipant() {
		var participantId = "T" + Get.temp();
		var participant = participantById.computeIfAbsent(participantId, p -> new ExParticipant());

		return new Participant() {
			public Order order(String symbol) {
				return order(symbol, isHedged ? "" : "P" + Get.temp());
			}

			private Order order(String symbol, String positionId) {
				var symbolPositionId = symbol + "#" + positionId;
				var orderId = "O" + Get.temp();
				var key = participantId + ":" + symbolPositionId + ":" + orderId;
				var lob = lob(symbol);
				var orderMutable = Mutable.<LimitOrderBook<String>.Order> nil();

				var position = new Position() {
					public Order close() {
						return closePartially(participant.getPosition(symbolPositionId).getBuySell());
					}

					public synchronized Order closePartially(int buySell) {
						var order = closeOrder();
						order.new_(-buySell, Float.NaN);
						if (order.fulfilled() == -buySell)
							return order;
						else
							throw new RuntimeException();
					}

					private Order closeOrder() {
						return order(symbol, positionId);
					}
				};

				return new Order() {
					public synchronized void new_(int buySell, float price) {
						var order = lob.new Order(key, price, buySell);
						update(null, order);
						participant.putOrder(orderId, order);
					}

					public synchronized String amend(int buySell, float price) {
						var order0 = orderMutable.value();
						var orderx = lob.new Order(key, price, buySell);
						orderx.xBuySell = order0.xBuySell;

						if (Math.abs(orderx.xBuySell) < Math.abs(orderx.buySell)) { // not yet fullfilled
							update(order0, orderx);
							participant.putOrder(orderId, orderx);
							return key;
						} else
							throw new RuntimeException();
					}

					public synchronized void cancel() {
						var order0 = orderMutable.value();
						update(order0, null);
						participant.removeOrder(orderId);
					}

					public int fulfilled() {
						return orderMutable.value().xBuySell;
					}

					public Position position() {
						return position;
					}

					private void update(LimitOrderBook<String>.Order order0, LimitOrderBook<String>.Order orderx) {
						lob.update(order0, orderx);
						orderMutable.update(orderx);
					}
				};
			}

			public ExSummary summary() {
				return participant.summary(symbol -> lob(symbol).getLastPrice(), invLeverage);
			}
		};
	}

	private LimitOrderBook<String> lob(String symbol) {
		var marketData = marketDataBySymbol.computeIfAbsent(symbol, s -> new MarketData());

		return lobBySymbol.computeIfAbsent(symbol, s -> new LimitOrderBook<>(new LobListener<>() {
			public void handleOrderFulfilled(LimitOrderBook<String>.Order order, float price, int buySell) {
				pspo(order).map((participantId, symbolPositionId, orderId) -> participantById //
						.get(participantId) //
						.record(buySell, symbolPositionId, price, isLeveraged));
			}

			public void handleOrderDisposed(LimitOrderBook<String>.Order order) {
				pspo(order).map((participantId, symbolPositionId, orderId) -> participantById //
						.get(participantId) //
						.removeOrder(orderId));
			}

			public void handleQuoteChanged(float bid, float ask, int volume) {
				marketData.update(System.currentTimeMillis(), (bid + ask) * .5f, volume);
			}
		}));
	}

	private FixieArray<String> pspo(LimitOrderBook<String>.Order order) {
		return pspo(order.key);
	}

	private FixieArray<String> pspo(String key) {
		return FixieArray.of(key.split(":"));
	}

}
