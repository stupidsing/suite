package suite.trade.data;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Fit;
import primal.MoreVerbs.Read;
import primal.Verbs.Build;
import primal.adt.Pair;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import primal.streamlet.Streamlet;
import suite.http.HttpClient;
import suite.node.util.Singleton;

public class Sina {

	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	public class Factor {
		public String symbol;
		public String name;
		public float open;
		public float close;
		public float high;
		public float low;
		public float quote;
		public float change;
		public float changePercent;
		public float bid;
		public float ask;
		public float volumeHkd;
		public float volume; // in stock
		public float pe;
		public float weeklyInterest;
		public float high52week;
		public float low52week;
		public String lastCloseDate; // 2017/07/07
		public String lastCloseTime; // 16:08:44
	}

	public synchronized Map<String, Float> quote(Set<String> symbols) {
		return quoteCache.quote(symbols, "-");
	}

	public Factor queryFactor(String symbol) {
		return queryFactors(Read.each(symbol), true).uniqueResult();
	}

	public int queryLotSize(String symbol) {
		return queryLotSizes(Read.each(symbol), true).get(symbol);
	}

	private Map<String, Float> quote_(Streamlet<String> symbols, String dummy) {
		return queryFactors(symbols, false).toMap(factor -> factor.symbol, factor -> factor.quote);
	}

	// http://blog.sina.com.cn/s/blog_5dc29fcc0101dq5s.html
	public Streamlet<Factor> queryFactors(Streamlet<String> symbols, boolean isCache) {
		return 0 < symbols.size() ? queryFactor_(symbols, isCache) : Read.empty();
	}

	public Map<String, Integer> queryLotSizes(Streamlet<String> symbols, boolean isCache) {
		return 0 < symbols.size() ? queryLotSizes_(symbols, isCache) : Map.ofEntries();
	}

	private Streamlet<Factor> queryFactor_(Streamlet<String> symbols, boolean isCache) {
		var url = "http://hq.sinajs.cn/?list=" + symbols //
				.map(this::toSina) //
				.toJoinedString(",");

		return getLines(url, isCache) //
				.map(line -> Fit.parts(line, "var hq_str_", "=\"", "\"").map((t0, t1, t2) -> {

					// var hq_str_rt_hk00005="xxx";
					// where xxx is a single comma-separated line like this

					// HSBC HOLDINGS,XXX,
					// 73.250,73.350,73.700,73.050, 73.150,
					// -0.200,-0.273,
					// 73.150,73.200,
					// 1439148052.782,19650107,
					// 77.098,0.697,
					// 74.500,46.350,
					// 2017/07/07,16:08:44,
					// 100|0,N|Y|Y,73.200|69.600|75.450,0|||0.000|0.000|0.000,
					// |0,Y

					var vs = t2.split(",");

					var factor = new Factor();
					factor.symbol = toYahoo(t1);
					factor.name = vs[0];
					factor.open = Float.parseFloat(vs[2]);
					factor.close = Float.parseFloat(vs[3]);
					factor.high = Float.parseFloat(vs[4]);
					factor.low = Float.parseFloat(vs[5]);
					factor.quote = Float.parseFloat(vs[6]);
					factor.change = Float.parseFloat(vs[7]);
					factor.changePercent = Float.parseFloat(vs[8]);
					factor.bid = Float.parseFloat(vs[9]);
					factor.ask = Float.parseFloat(vs[10]);
					factor.volumeHkd = Float.parseFloat(vs[11]);
					factor.volume = Float.parseFloat(vs[12]); // in stock
					factor.pe = Float.parseFloat(vs[13]);
					factor.weeklyInterest = Float.parseFloat(vs[14]);
					factor.high52week = Float.parseFloat(vs[15]);
					factor.low52week = Float.parseFloat(vs[16]);
					factor.lastCloseDate = vs[17]; // 2017/07/07
					factor.lastCloseTime = vs[18]; // 16:08:44
					return factor;
				})) //
				.collect();
	}

	private Map<String, Integer> queryLotSizes_(Streamlet<String> symbols, boolean isCache) {
		var url = "http://hq.sinajs.cn/?list=" + symbols //
				.map(this::toSinaI) //
				.toJoinedString(",");

		return getLines(url, isCache) //
				.map(line -> Fit.parts(line, "var hq_str_", "=\"", "\"").map((t0, t1, t2) -> {

					// var hq_str_hk00005_i="xxx";
					// where xxx is a single comma-separated line like this

					// EQTY,MAIN,70.500,55.300,4.935,0,0,20637854696,0,20637854696,0,0.00,99529176645.00,4.4695,1,�����ع�,0,0.05,400,

					var vs = t2.split(",");

					return Pair.of(toYahooI(t1), Integer.parseInt(vs[18]));
				})) //
				.toMap(Pair::fst, Pair::snd);
	}

	private Streamlet<String> getLines(String url, boolean isCache) {
		return Read //
				.from(ex(() -> {
					Puller<Bytes> in;

					if (isCache)
						in = Singleton.me.storeCache.http(url);
					else
						in = HttpClient.get(url).out();

					return in //
							.map(bytes -> Build.string(sb -> {
								for (var i = 0; i < bytes.size(); i++)
									sb.append((char) bytes.get(i));
							})) //
							.toJoinedString() //
							.split("\n");
				}));
	}

	private String toYahoo(String sina) {
		var prefix = "rt_hk0";
		return sina.startsWith(prefix) ? sina.substring(prefix.length()) + ".HK" : fail(sina);
	}

	private String toYahooI(String sina) {
		var prefix = "hk0";
		var suffix = "_i";
		return sina.startsWith(prefix) && sina.endsWith(suffix)
				? sina.substring(prefix.length(), sina.length() - suffix.length()) + ".HK"
				: fail(sina);
	}

	private String toSina(String symbol) {
		return "rt_hk0" + symbol.substring(0, 4);
	}

	private String toSinaI(String symbol) {
		return "hk0" + symbol.substring(0, 4) + "_i";
	}

}
