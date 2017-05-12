package suite.trade;

public class Trade {

	public final String date;
	public final int buySell;
	public final String symbol;
	public final float price;
	public final String strategy;

	public static Trade of(String[] array) {
		return new Trade(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
	}

	public static Trade of(int buySell, String symbol, float price) {
		return of(buySell, symbol, price, "-");
	}

	public static Trade of(int buySell, String symbol, float price, String strategy) {
		return new Trade("-", buySell, symbol, price, strategy);
	}

	private Trade(String date, int buySell, String symbol, float price, String strategy) {
		this.date = date;
		this.buySell = buySell;
		this.symbol = symbol;
		this.price = price;
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		return "|" + symbol + ":" + price + "*" + buySell;
	}

}
