package suite.math.stat;

public class Quant {

	public static double logReturn(double price0, double price1) {
		return Math.log1p(return_(price0, price1));
	}

	public static double return_(double price0, double price1) {
		return (price1 - price0) / price0;
	}

}
