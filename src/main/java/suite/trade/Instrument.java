package suite.trade;

import primal.Verbs.Equals;
import suite.node.util.Singleton;
import suite.serialize.Serialize.Serializer;
import suite.trade.data.Hkex;

public class Instrument {

	public static String cashSymbol = "HKD";
	public static String hsiSymbol = "^HSI";

	public static Instrument cash = new Instrument(cashSymbol, "Hong Kong Dollar", 1, Integer.MAX_VALUE);
	public static Instrument hsi = Instrument.of(hsiSymbol, "Hang Seng Index", 1);

	public static Serializer<Instrument> serializer = Singleton.me.serialize.auto(Instrument.class);

	public final String symbol;
	public final String name;
	public final int lotSize;
	public final int marketCap; // HKD million

	public static Instrument of(String symbol, String name, int lotSize) {
		return of(symbol, name, lotSize, 0);
	}

	public static Instrument of(String symbol, String name, int lotSize, int marketCap) {
		return new Instrument(symbol, name, lotSize, marketCap);
	}

	private Instrument(String symbol, String name, int lotSize, int marketCap) {
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
		if (object.getClass() == Instrument.class) {
			var other = (Instrument) object;
			return Equals.string(symbol, other.symbol);
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
