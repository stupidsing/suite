package suite.trade;

import java.util.ArrayList;
import java.util.List;

import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Serialize;
import suite.util.Util;

// extract from HKEx fact book
// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class Hkex2012 {

	public static void main(String[] args) {
		System.out.println(new Hkex2012().queryLeadingCompaniesByMarketCap().toList());
	}

	public Streamlet<Asset> queryLeadingCompaniesByMarketCap() {
		return queryLeadingCompaniesByMarketCap("2012");
	}

	private Streamlet<Asset> queryLeadingCompaniesByMarketCap(String year) {
		return Read.from(SerializedStoreCache //
				.of(Serialize.list(Asset.serializer)) //
				.get(getClass().getSimpleName() + ".queryLeadingCompaniesByMarketCap(" + year + ")",
						() -> queryLeadingCompaniesByMarketCap0(year)));
	}

	private List<Asset> queryLeadingCompaniesByMarketCap0(String year) {
		String url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook" + year + "/Documents/06.pdf";

		String cmd = "" //
				+ "wget -q -O - '" + url + "'" //
				+ " | pdftotext -nopgbrk -raw - -" //
				+ " | sed -e '1,/leading companies in market capitalisation/ d'" //
				+ " | grep '^[1-9]'" //
				+ " | cut -d\\, -f1" //
				+ " | sed 's/\\(.*\\) [0-9]*$/\\1/g'";

		String out = Execute.shell(cmd);

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
						return new Asset(Util.right("0000" + list.get(1).replace("*", "").trim(), -4) + ".HK", list.get(2));
					} else
						return null;
				}) //
				.filter(asset -> asset != null) //
				.toList();
	}

}
