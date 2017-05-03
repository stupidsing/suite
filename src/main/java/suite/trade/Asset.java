package suite.trade;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import suite.util.Serialize.Serializer;

public class Asset {

	public static Serializer<Asset> serializer = new Serializer<Asset>() {
		public Asset read(DataInput dataInput) throws IOException {
			String code = dataInput.readUTF();
			String name = dataInput.readUTF();
			int marketCap = dataInput.readInt();
			return new Asset(code, name, marketCap);
		}

		public void write(DataOutput dataOutput, Asset asset) throws IOException {
			dataOutput.writeUTF(asset.code);
			dataOutput.writeUTF(asset.name);
			dataOutput.writeInt(asset.marketCap);
		}
	};

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
