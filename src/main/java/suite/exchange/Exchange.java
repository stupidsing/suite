package suite.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Get;
import primal.adt.FixieArray;
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
		public ExSummary getSummary();

		public String orderNew(int buySell, String symbol, float price);

		public String orderAmend(String key, int buySell, float price);

		public String positionClose(String key, float price);

		public String positionClosePartially(String key, int buySell, float price);
	}

	public static FixieArray<String> sp(String symbolPositionId) {
		return FixieArray.of(symbolPositionId.split("#"));
	}

	public Participant newParticipant() {
		var participantId = "T" + Get.temp();
		var participant = participantById.computeIfAbsent(participantId, p -> new ExParticipant());

		return new Participant() {
			public String orderNew(int buySell, String symbol, float price) {
				var positionId = isHedged ? "" : "P" + Get.temp();
				return submitOrder(participantId, positionId, buySell, symbol, price);
			}

			public String orderAmend(String key, int buySell, float price) {
				return pspo(key).map(
						(participantId, symbolPositionId, orderId) -> sp(symbolPositionId).map((symbol, positionId) -> {
							var lob = lob(symbol);
							var order0 = participant.getOrder(orderId);
							var orderx = lob.new Order(key, price, buySell);

							lob.update(order0, orderx);
							participant.putOrder(orderId, orderx);
							return key;
						}));
			}

			public String positionClose(String key, float price) {
				return pspo(key).map((participantId, symbolPositionId, orderId) -> {
					var buySell = participant.getPosition(symbolPositionId).getBuySell();
					return positionClosePartially(key, buySell, price);
				});
			}

			public String positionClosePartially(String key, int buySell, float price) {
				return submitOrder(key, -buySell, price);
			}

			public ExSummary getSummary() {
				return participant.summary(symbol -> lob(symbol).getLastPrice(), invLeverage);
			}
		};
	}

	private String submitOrder(String key, int buySell, float price) {
		return pspo(key).map((participantId, symbolPositionId, orderId) -> sp(symbolPositionId)
				.map((symbol, positionId) -> submitOrder(participantId, positionId, buySell, symbol, price)));
	}

	private String submitOrder(String participantId, String positionId, int buySell, String symbol, float price) {
		var orderId = "O" + Get.temp();
		var symbolPositionId = symbol + "#" + positionId;
		var key = participantId + ":" + symbolPositionId + ":" + orderId;
		var lob = lob(symbol);
		var order = lob.new Order(key, price, buySell);

		lob.update(null, order);
		participantById.get(participantId).putOrder(orderId, order);
		return key;
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
