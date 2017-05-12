package suite.trade;

import suite.trade.data.Hkex;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.String_;

public class Asset {

	public static String cashCode = "HKD";
	public static Asset cash = new Asset(cashCode, "Hong Kong Dollar", 1, Integer.MAX_VALUE);

	public static Serializer<Asset> serializer = Serialize.auto(Asset.class);

	public final String symbol;
	public final String name;
	public final int lotSize;
	public final int marketCap; // HKD million

	public static Asset of(String code, String name, int lotSize) {
		return of(code, name, lotSize, 0);
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
			Asset other = (Asset) object;
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
		String[] array = name.split(" ");
		int i = 0;
		String s = "", name = "";
		while (Hkex.commonFirstNames.contains(s) && i < array.length)
			name += s = array[i++];
		return name;
	}

}
