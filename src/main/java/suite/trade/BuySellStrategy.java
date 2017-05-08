package suite.trade;

/**
 * Strategy that advise you to buy or sell on one specific stock.
 *
 * @author ywsing
 */
public interface BuySellStrategy {

	public GetBuySell analyze(float[] prices);

	// 1 = buy, 0 = no change, -1 = sell
	public interface GetBuySell {
		public int get(int d);
	}

}
