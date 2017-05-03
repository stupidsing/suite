package suite.trade;

import java.util.ArrayList;
import java.util.List;

import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Serialize;

// extract from HKEx fact book
// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class Hkex2012 {

	public static void main(String[] args) {
		List<Asset> assets = new Hkex2012().queryLeadingCompaniesByMarketCapitalisation().toList();
		System.out.println(assets);
	}

	public Streamlet<Asset> queryLeadingCompaniesByMarketCapitalisation() {
		return Read.from(SerializedStoreCache //
				.of(Serialize.list(Asset.serializer)) //
				.get("Hkex2012.queryLeadingCompaniesByMarketCapitalisation", () -> queryLeadingCompaniesByMarketCapitalisation0()));
	}

	private List<Asset> queryLeadingCompaniesByMarketCapitalisation0() {
		String url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/Documents/06.pdf";

		String cmd = "" //
				+ "wget -q -O - '" + url + "'" //
				+ " | pdftotext -nopgbrk -raw - -" //
				+ " | sed -e '1,/leading companies in market capitalisation/ d'" //
				+ " | grep '^[1-9]'" //
				+ " | cut -d\\, -f1" //
				+ " | sed 's/\\(.*\\) [0-9]*$/\\1/g'";

		String out = Execute.shell(cmd);

		System.out.println(out);

		return Read.from(out.split("\n")) //
				.map(line -> {
					int p0 = line.indexOf(" ", 0);
					int p1 = 0 <= p0 ? line.indexOf(" ", p0 + 1) : -1;
					System.out.println(p0);
					System.out.println(p1);
					if (0 <= p1) {
						List<String> list = new ArrayList<>();
						int[] ps = { p0, p1, };
						int s = 0;
						for (int p : ps) {
							list.add(line.substring(s, p));
							s = p + 1;
						}
						list.add(line.substring(s));
						return new Asset(list.get(1), list.get(2));
					} else
						return null;
				}) //
				.filter(asset -> asset != null) //
				.collect(As::streamlet);
	}

}
