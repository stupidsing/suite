package suite.trade.data;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;
import suite.util.To;

// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class HkexFactBook {

	public Streamlet<String> queryDelisted() {
		String url = "http://www.hkexnews.hk/reports/prolongedsusp/Documents/psuspenrep_mb.doc";

		return Singleton.me.storeCache //
				.pipe(url) //
				.pipe("xargs -I {} curl '{}'") //
				.pipe("catdoc") //
				.pipe("sed -n 's/.*(\\(.*\\)).*/\\1/p'") //
				.pipe("egrep -v '^[A-Za-z]'") //
				.pipe("sort -rn") //
				.read() //
				.map(HkexUtil::toSymbol) //
				.collect(As::streamlet);
	}

	public Streamlet<String> queryLeadingCompaniesByMarketCap(int year) {
		List<List<String>> list0 = Singleton.me.storeCache //
				.pipe(getUrl(year)) //
				.pipe("xargs -I {} curl '{}'") //
				.pipe("pdftotext -nopgbrk -raw - -") //
				.pipe("sed -e '1,/leading companies in market capitalisation/ d'") //
				.pipe("grep '^[1-9]'") //
				.pipe("cut -d\\, -f1") //
				.pipe("sed 's/\\(.*\\) [0-9]*$/\\1/g'") //
				.read() //
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
						return Read.each(list);
					} else
						return Read.empty();
				}) //
				.toList();

		List<List<String>> list1 = new ArrayList<>();
		int i = 1;

		for (List<String> list_ : list0)
			if (Integer.parseInt(list_.get(0)) == i++)
				list1.add(list_);
			else
				break;

		return Read.from(list1) //
				.map(list -> {
					// Asset asset = new
					// Asset(HkexUtil.toSymbol(list.get(1).replace("*",
					// "")), list.get(2));
					return HkexUtil.toSymbol(list.get(1).replace("*", ""));
				}) //
				.collect(As::streamlet);
	}

	public Streamlet<String> queryMainBoardCompanies(int year) {
		return Singleton.me.storeCache //
				.pipe(getUrl(year)) //
				.pipe("xargs -I {} curl '{}'") //
				.pipe("pdftotext -nopgbrk -raw - -") //
				.pipe("sed -e '1,/List of listed companies on Main Board/ d'") //
				.pipe("sed -n '1,/List of listed companies on GEM/ p'") //
				.pipe("egrep '^0'") //
				.read() //
				.map(line -> HkexUtil.toSymbol(line.substring(0, 5))) //
				.sort(Object_::compare) //
				.collect(As::streamlet);
	}

	private String getUrl(int year) {
		String dir = "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book";
		if (year <= 2008)
			return dir + "/HKEX-Fact-Book-" + year + "/FB_" + year + ".pdf";
		else if (year <= 2015)
			return dir + "/HKEx-Fact-Book-" + year + "/fb_" + year + ".pdf";
		else if (year == 2016)
			return dir + "/HKEX-Fact-Book-" + year + "/FB_" + year + ".pdf";
		else
			throw new RuntimeException();
	}

	@SuppressWarnings("unused")
	private String getUrl0(int year, String section) {
		URI uri0 = To.uri("https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook.htm");
		Map<String, URI> links0 = HttpUtil.resolveLinks(uri0);
		URI uri1 = links0.get(Integer.toString(year));
		Map<String, URI> links1 = HttpUtil.resolveLinks(uri1);

		for (Entry<String, URI> e : links1.entrySet()) {
			String link = e.getKey();
			if (link.startsWith(section) || link.startsWith("- " + section))
				return e.getValue().toString();
		}

		throw new RuntimeException();
	}

}
