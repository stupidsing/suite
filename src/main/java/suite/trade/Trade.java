package suite.trade;

import static java.lang.Math.abs;

import primal.String_;
import primal.Verbs.Compare;
import suite.math.Math_;
import suite.util.To;

public class Trade {

	public static String NA = "-";

	public final String date;
	public final int buySell;
	public final String symbol;
	public final float price;
	public final String strategy;
	public final String remark;

	public static int compare(Trade trade0, Trade trade1) {
		var c = 0;
		c = c == 0 ? Compare.objects(trade0.symbol, trade1.symbol) : c;
		c = c == 0 ? Integer.compare(trade0.buySell, trade1.buySell) : c;
		c = c == 0 ? Compare.objects(trade0.strategy, trade1.strategy) : c;
		return c;
	};

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
		String date_, remark_;

		if (date.endsWith("#")) {
			date_ = date.substring(0, date.length() - 1);
			remark_ = "#";
		} else {
			date_ = date;
			remark_ = "-";
		}

		this.date = date_;
		this.buySell = buySell;
		this.symbol = symbol;
		this.price = price;
		this.strategy = strategy;
		this.remark = remark_;
	}

	public String record() {
		return date //
				+ (!String_.equals(remark, "-") ? remark : "") //
				+ "\t" + buySell //
				+ "\t" + symbol //
				+ "\t" + price //
				+ "\t" + strategy;
	}

	public float amount() {
		return buySell * price;
	}

	@Override
	public String toString() {
		return (!String_.equals(date, NA) ? date + " " : "") //
				+ Math_.posNeg(buySell) //
				+ symbol //
				+ ":" + To.string(price) + "*" + abs(buySell);
	}

}
