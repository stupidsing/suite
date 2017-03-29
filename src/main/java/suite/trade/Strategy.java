package suite.trade;

public interface Strategy {

	public GetBuySell analyze(double prices[]);

	// 1 = buy, 0 = no change, -1 = sell
	public interface GetBuySell {
		public int get(int d);
	}

}
