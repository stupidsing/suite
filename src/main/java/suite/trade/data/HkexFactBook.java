package suite.trade.data;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Opt;
import suite.http.HttpUtil;
import suite.os.Execute;
import suite.os.SerializedStoreCache;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;
import suite.util.Serialize;
import suite.util.To;

// https://www.hkex.com.hk/eng/stat/statrpt/factbook/factbook2012/fb2012.htm
public class HkexFactBook {

	private String prefix = getClass().getSimpleName();
	private Serialize serialize = Serialize.me;

	public Streamlet<String> queryDelisted() {
		return Read.from(SerializedStoreCache //
				.of(serialize.list(serialize.variableLengthString)) //
				.get(prefix + ".queryDelisted()", () -> {
					String cmd = "" //
							+ "curl http://www.hkexnews.hk/reports/prolongedsusp/Documents/psuspenrep_mb.doc" //
							+ " | catdoc" //
							+ " | sed -n 's/.*(\\(.*\\)).*/\\1/p'" //
							+ " | egrep -v '^[A-Za-z]'" //
							+ " | sort -rn";

					return Opt.of(cmd).map(Execute::shell).map(s -> s.split("\n")).map(Arrays::asList).get();
				})) //
				.map(HkexUtil::toSymbol);
	}

	public Streamlet<String> queryLeadingCompaniesByMarketCap(int year) {
		return Read.from(SerializedStoreCache //
				.of(serialize.list(serialize.variableLengthString)) //
				.get(prefix + ".queryLeadingCompaniesByMarketCap(" + year + ")", () -> queryLeadingCompaniesByMarketCap_(year)));
	}

	private List<String> queryLeadingCompaniesByMarketCap_(int year) {
		String url = getUrl(year);

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

	public Streamlet<String> queryMainBoardCompanies(int year) {
		return Read.from(SerializedStoreCache //
				.of(serialize.list(serialize.variableLengthString)) //
				.get(prefix + ".queryMainBoardCompanies(" + year + ")", () -> {
					String url = getUrl(year);
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
				}));
	}

	private String getUrl(int year) {
		if (year == 2011)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEx-Fact-Book-2011/fb_2011.pdf?la=en";
		else if (year == 2012)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEx-Fact-Book-2012/fb_2012.pdf?la=en";
		else if (year == 2013)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEx-Fact-Book-2013/fb_2013.pdf?la=en";
		else if (year == 2014)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEX-Fact-Book-2014/fb_2014.pdf?la=en";
		else if (year == 2015)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEX-Fact-Book-2015/fb_2015.pdf?la=en";
		else if (year == 2016)
			return "http://www.hkex.com.hk/-/media/HKEX-Market/Market-Data/Statistics/Consolidated-Reports/HKEX-Fact-Book/HKEX-Fact-Book-2016/FB_2016.pdf?la=en";
		else
			throw new RuntimeException();
	}

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
