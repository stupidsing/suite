package suite.trade.data;

import java.util.ArrayList;
import java.util.List;

import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;
import suite.util.Serialize;

// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class HkexFactBook {

	public Streamlet<String> queryLeadingCompaniesByMarketCap(int year) {
		return Read.from(SerializedStoreCache //
				.of(Serialize.list(Serialize.variableLengthString)) //
				.get(getClass().getSimpleName() + ".queryLeadingCompaniesByMarketCap(" + year + ")",
						() -> queryLeadingCompaniesByMarketCap_(year)));
	}

	private List<String> queryLeadingCompaniesByMarketCap_(int year) {
		String url;

		if (2009 <= year)
			url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook" + year + "/Documents/06.pdf";
		else if (2004 <= year)
			url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook" + year + "/e/06.pdf";
		else if (2003 <= year)
			url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook/documents/06.pdf";
		else if (2001 <= year)
			url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook" + year + "/documents/06.pdf";
		else if (2000 <= year)
			url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook" + year + "/documents/04c_Market%20Capitalisation.pdf";
		else
			throw new RuntimeException("record not exist");

		String cmd = "" //
				+ "curl '" + url + "'" //
				+ " | pdftotext -nopgbrk -raw - -" //
				+ " | sed -e '1,/leading companies in market capitalisation/ d'" //
				+ " | grep '^[1-9]'" //
				+ " | cut -d\\, -f1" //
				+ " | sed 's/\\(.*\\) [0-9]*$/\\1/g'";

		String out = Execute.shell(cmd);

		return Read.from(out.split("\n")) //
				.concatMap(line -> {
					int p0 = line.indexOf(" ", 0);
					int p1 = 0 <= p0 ? line.indexOf(" ", p0 + 1) : -1;
					if (0 <= p1) {
						List<String> list = new ArrayList<>();
						int[] ps = { p0, p1, };
						int s = 0;
						for (int p : ps) {
							list.add(line.substring(s, p));
							s = p + 1;
						}
						list.add(line.substring(s));
						// Asset asset = new
						// Asset(HkexUtil.toSymbol(list.get(1).replace("*",
						// "")), list.get(2));
						return Read.each(HkexUtil.toSymbol(list.get(1).replace("*", "")));
					} else
						return Read.empty();
				}) //
				.toList();
	}

	public List<String> queryMainBoardCompanies() {
		String url = "https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2016/Documents/38.pdf";
		String cmd = "" //
				+ "curl '" + url + "'" //
				+ " | pdftotext -nopgbrk -raw - -" //
				+ " | sed -e '1,/List of listed companies on Main Board/ d'" //
				+ " | sed -n '1,/List of listed companies on GEM/ p'" //
				+ " | egrep '^0'";

		String out = Execute.shell(cmd);

		return Read.from(out.split("\n")) //
				.map(line -> HkexUtil.toSymbol(line.substring(0, 5))) //
				.sort(Object_::compare) //
				.toList();
	}

}
