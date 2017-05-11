package suite.trade.data;

import suite.util.Util;

public class HkexUtil {

	public static String toStockCode(String symbol) {
		return "" + Integer.parseInt(symbol.replace(".HK", ""));
	}

	public static String toSymbol(String stockCode) {
		return Util.right("0000" + stockCode.trim(), -4) + ".HK";
	}

}
