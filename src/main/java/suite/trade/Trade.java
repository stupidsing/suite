package suite.trade;

public class Trade {

	public final String date;
	public final int buySell;
	public final String stockCode;
	public final float price;
	public final String strategy;

	public Trade(String[] array) {
		this(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
	}

	public Trade(String date, int buySell, String stockCode, float price, String strategy) {
		this.date = date;
		this.buySell = buySell;
		this.stockCode = stockCode;
		this.price = price;
		this.strategy = strategy;
	}

}
