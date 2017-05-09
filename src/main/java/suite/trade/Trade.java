package suite.trade;

public class Trade {

	public final String date;
	public final int buySell;
	public final String symbol;
	public final float price;
	public final String strategy;

	public Trade(String[] array) {
		this(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
	}

	public Trade(int buySell, String symbol, float price) {
		this(buySell, symbol, price, "-");
	}

	public Trade(int buySell, String symbol, float price, String strategy) {
		this("-", buySell, symbol, price, strategy);
	}

	public Trade(String date, int buySell, String symbol, float price, String strategy) {
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
