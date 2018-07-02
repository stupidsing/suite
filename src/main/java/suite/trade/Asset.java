package suite.trade;

import suite.node.util.Singleton;
import suite.serialize.Serialize.Serializer;
import suite.trade.data.Hkex;
import suite.util.String_;

public class Asset {

	public static String cashSymbol = "HKD";
	public static String hsiSymbol = "^HSI";

	public static Asset cash = new Asset(cashSymbol, "Hong Kong Dollar", 1, Integer.MAX_VALUE);
	public static Asset hsi = Asset.of(hsiSymbol, "Hang Seng Index", 1);

	public static Serializer<Asset> serializer = Singleton.me.serialize.auto(Asset.class);

	public final String symbol;
	public final String name;
	public final int lotSize;
	public final int marketCap; // HKD million

	public static Asset of(String symbol, String name, int lotSize) {
		return of(symbol, name, lotSize, 0);
	}

	public static Asset of(String symbol, String name, int lotSize, int marketCap) {
		return new Asset(symbol, name, lotSize, marketCap);
	}

	private Asset(String symbol, String name, int lotSize, int marketCap) {
		this.symbol = symbol;
		this.name = name;
		this.lotSize = lotSize;
		this.marketCap = marketCap;
	}

	public String shortName() {
		return shortName_();
	}

	@Override
	public boolean equals(Object object) {
		if (object.getClass() == Asset.class) {
			var other = (Asset) object;
			return String_.equals(symbol, other.symbol);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return symbol.hashCode();
	}

	@Override
	public String toString() {
		return symbol + " " + shortName();
	}

	private String shortName_() {
		var array = name.split(" ");
		var i = 0;
		String s = "", name = "";
		while (Hkex.commonFirstNames.contains(s) && i < array.length)
			name += s = array[i++];
		return name;
	}

}
