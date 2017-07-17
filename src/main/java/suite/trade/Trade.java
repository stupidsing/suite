package suite.trade;

import suite.math.MathUtil;
import suite.util.String_;
import suite.util.To;

public class Trade {

	public static String NA = "-";

	public final String date;
	public final int buySell;
	public final String symbol;
	public final float price;
	public final String strategy;

	public static Trade of(String[] array) {
		return new Trade(array[0], Integer.parseInt(array[1]), array[2], Float.parseFloat(array[3]), array[4]);
	}

	public static Trade of(int buySell, String symbol, float price) {
		return of(NA, buySell, symbol, price, "-");
	}

	public static Trade of(String date, int buySell, String symbol, float price, String strategy) {
		return new Trade(date, buySell, symbol, price, strategy);
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
		return (!String_.equals(date, NA) ? date + " " : "") //
				+ MathUtil.posNeg(buySell) //
				+ symbol //
				+ ":" + To.string(price) + "*" + Math.abs(buySell);
	}

}
