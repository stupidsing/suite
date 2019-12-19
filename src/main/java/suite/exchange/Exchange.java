package suite.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Get;
import primal.adt.FixieArray;
import suite.exchange.LimitOrderBook.LobListener;

public class Exchange {

	private boolean isLeveraged = true;
	private int leverage = !isLeveraged ? 1 : 100;
	private double invLeverage = 1d / leverage;

	private Map<String, ExParticipant> participantById = new ConcurrentHashMap<>();
	private Map<String, LimitOrderBook<String>> lobBySymbol = new ConcurrentHashMap<>();
	private Map<String, MarketData> marketDataBySymbol = new ConcurrentHashMap<>();

	public interface Participant {
		public ExSummary getSummary();

		public String orderNew(int buySell, String symbol, float price);

		public String positionClose(String symbolPositionId, float price);

		public String positionClosePartially(String participantId, String symbolPositionId, int buySell, float price);
	}

	public static FixieArray<String> sp(String symbolPositionId) {
		return FixieArray.of(symbolPositionId.split("#"));
	}

	public Participant newParticipant() {
		var participantId = "T" + Get.temp();
		var participant = participantById.computeIfAbsent(participantId, p -> new ExParticipant());

		return new Participant() {
			public String orderNew(int buySell, String symbol, float price) {
				var positionId = !isLeveraged ? "" : "P" + Get.temp();
				return submitOrder(participantId, positionId, buySell, symbol, price);
			}

			public String positionClose(String symbolPositionId, float price) {
				var buySell = participant.getPosition(symbolPositionId).getBuySell();
				return positionClosePartially(participantId, symbolPositionId, -buySell, price);
			}

			public String positionClosePartially(String participantId, String symbolPositionId, int buySell,
					float price) {
				return sp(symbolPositionId)
						.map((symbol, positionId) -> submitOrder(participantId, positionId, buySell, symbol, price));
			}

			public ExSummary getSummary() {
				return participant.summary(symbol -> lob(symbol).getLastPrice(), invLeverage);
			}
		};
	}

	private String submitOrder(String participantId, String positionId, int buySell, String symbol, float price) {
		var orderId = "O" + Get.temp();
		var symbolPositionId = symbol + "#" + positionId;
		var lob = lob(symbol);

		var order = lob.new Order();
		order.key = participantId + ":" + symbolPositionId + ":" + orderId;
		order.price = price;
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
						.record(buySell, symbolPositionId, price, isLeveraged));
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
		return FixieArray.of(order.key.split(":"));
	}

}
