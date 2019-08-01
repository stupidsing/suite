package suite.trade.data;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import primal.Verbs.Compare;
import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class HkexFactBook {

	public Streamlet<String> queryDelisted() {
		var url = "http://www.hkexnews.hk/reports/prolongedsusp/Documents/psuspenrep_mb.doc";

		return Singleton.me.storeCache //
				.pipe(url) //
				.pipe("xargs -I {} curl '{}'") //
				.pipe("catdoc") //
				.pipe("sed -n 's/.*(\\(.*\\)).*/\\1/p'") //
				.pipe("egrep -v '^[A-Za-z]'") //
				.pipe("sort -rn") //
				.read() //
				.map(HkexUtil::toSymbol) //
				.collect();
	}

	public Streamlet<String> queryCompaniesByMarketCap(int year) {
		return year <= 2017 //
				? queryYearlyLeadingCompaniesByMarketCap(year) //
				: queryQuarterlyLeadingCompaniesByMarketCap(year, "4th-Quarter");
	}

	public Streamlet<String> queryYearlyLeadingCompaniesByMarketCap(int year) {
		var list0 = Singleton.me.storeCache //
				.pipe(getUrl(year)) //
				.pipe("xargs -I {} curl '{}'") //
				.pipe("pdftotext -nopgbrk -raw - -") //
				.pipe("sed -e '1,/leading companies in market capitalisation/ d'") //
				.pipe("grep '^[1-9]'") //
				.pipe("cut -d\\, -f1") //
				.pipe("sed 's/\\(.*\\) [0-9]*$/\\1/g'") //
				.read() //
				.concatMap(line -> {
					var p0 = line.indexOf(" ", 0);
					var p1 = 0 <= p0 ? line.indexOf(" ", p0 + 1) : -1;
					if (0 <= p1) {
						var list = new ArrayList<String>();
						int[] ps = { p0, p1, };
						var s = 0;
						for (var p : ps) {
							list.add(line.substring(s, p));
							s = p + 1;
						}
						list.add(line.substring(s));
						return Read.each(list);
					} else
						return Read.empty();
				}) //
				.toList();

		var list1 = new ArrayList<List<String>>();
		var i = 1;

		for (var list_ : list0)
			if (Integer.parseInt(list_.get(0)) == i++)
				list1.add(list_);
			else
				break;

		return Read //
				.from(list1) //
				.map(list -> {
					// var instrument = new
					// Instrument(HkexUtil.toSymbol(list.get(1).replace("*",
					// "")), list.get(2));
					return HkexUtil.toSymbol(list.get(1).replace("*", ""));
				}) //
				.collect();
	}

	public Streamlet<String> queryQuarterlyLeadingCompaniesByMarketCap(int year, String quarter) {
		String url = "https://www.hkex.com.hk" //
				+ "/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports" //
				+ "/HKEX-Securities-and-Derivatives-Markets-Quarterly-Report" //
				+ "/" + quarter + "-" + year + "/Full_e.pdf?la=en";

		return Singleton.me.storeCache //
				.pipe(url) //
				.pipe("xargs -I {} wget -O - '{}'") //
				.pipe("pdftotext -nopgbrk -raw - -") //
				.pipe("sed -e '1,/50 Leading Companies by Market Capitalisation/ d'") //
				.pipe("sed -n '1,/Market Total/ p'") //
				.pipe("cut -d' ' -f2-") //
				.pipe("egrep '^0'") //
				.read() //
				.map(line -> HkexUtil.toSymbol(line.substring(0, 5))) //
				.sort(Compare::objects) //
				.collect();
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
				.sort(Compare::objects) //
				.collect();
	}

	private String getUrl(int year) {
		var dir = "https://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book";
		if (year <= 2017)
			return dir + "/HKEX-Fact-Book-" + year + "/FB_" + year + ".pdf";
		else
			return fail();
	}

	@SuppressWarnings("unused")
	private String getUrl0(int year, String section) {
		var uri0 = ex(() -> new URI("https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook.htm"));
		var links0 = HttpUtil.resolveLinks(uri0);
		var uri1 = links0.get(Integer.toString(year));
		var links1 = HttpUtil.resolveLinks(uri1);

		for (var e : links1.entrySet()) {
			var link = e.getKey();
			if (link.startsWith(section) || link.startsWith("- " + section))
				return e.getValue().toString();
		}

		return fail();
	}

}
