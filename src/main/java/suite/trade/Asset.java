package suite.trade;

public class Asset {

	public final String code;
	public final String name;
	public final int marketCap; // HKD million

	public Asset(String code, String name) {
		this(code, name, 0);
	}

	public Asset(String code, String name, int marketCap) {
		this.code = code;
		this.name = name;
		this.marketCap = marketCap;
	}

	public String toString() {
		return code + " " + shortName();
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
