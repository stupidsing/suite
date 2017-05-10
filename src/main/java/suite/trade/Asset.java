package suite.trade;

import suite.trade.data.Hkex;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class Asset {

	public static String cashCode = "HKD";
	public static Asset cash = new Asset(cashCode, "Hong Kong Dollar");

	public static Serializer<Asset> serializer = Serialize.auto(Asset.class);

	public final String symbol;
	public final String name;
	public final int marketCap; // HKD million

	public Asset(String code, String name) {
		this(code, name, 0);
	}

	public Asset(String symbol, String name, int marketCap) {
		this.symbol = symbol;
		this.name = name;
		this.marketCap = marketCap;
	}

	public String toString() {
		return symbol + " " + shortName();
	}

	public String shortName() {
		String[] array = name.split(" ");
		int i = 0;
		String s = "", name = "";
		while (Hkex.commonFirstNames.contains(s) && i < array.length)
			name += s = array[i++];
		return name;
	}

}
