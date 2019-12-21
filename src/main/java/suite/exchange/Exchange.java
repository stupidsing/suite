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
		public Order positionClose(float price);

		public Order positionClosePartially(int buySell, float price);
	}

	public static FixieArray<String> sp(String symbolPositionId) {
		return FixieArray.of(symbolPositionId.split("#"));
	}

	public Participant newParticipant() {
		var participantId = "T" + Get.temp();
		var participant = participantById.computeIfAbsent(participantId, p -> new ExParticipant());

		return new Participant() {
			public Order order(String symbol) {
				var positionId = isHedged ? "" : "P" + Get.temp();
				var symbolPositionId = symbol + "#" + positionId;
				return order(symbol, symbolPositionId);
			}

			private Order order(String symbol, String symbolPositionId) {
				var orderId = "O" + Get.temp();
				var key = participantId + ":" + symbolPositionId + ":" + orderId;
				var lob = lob(symbol);
				var orderMutable = Mutable.<LimitOrderBook<String>.Order> nil();

				return new Order() {
					public void new_(int buySell, float price) {
						var order = lob.new Order(key, price, buySell);
						update(null, order);
						participant.putOrder(orderId, order);
					}

					public String amend(int buySell, float price) {
						var order0 = orderMutable.value();
						var orderx = lob.new Order(key, price, buySell);
						orderx.xBuySell = order0.xBuySell;

						if (Math.abs(orderx.xBuySell) < Math.abs(orderx.buySell)) {
							update(order0, orderx);
							participant.putOrder(orderId, orderx);
							return key;
						} else
							throw new RuntimeException();
					}

					public void cancel() {
						var order0 = orderMutable.value();
						update(order0, null);
						participant.removeOrder(orderId);
					}

					public int fulfilled() {
						return orderMutable.value().xBuySell;
					}

					public Position position() {
						return new Position() {
							public Order positionClose(float price) {
								var buySell = participant.getPosition(symbolPositionId).getBuySell();
								return positionClosePartially(buySell, price);
							}

							public Order positionClosePartially(int buySell, float price) {
								var order = order(symbol, symbolPositionId);
								order.new_(-buySell, price);
								return order;
							}
						};
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
