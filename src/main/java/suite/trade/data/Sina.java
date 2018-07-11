package suite.trade.data; import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.util.Map;
import java.util.Set;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.ParseUtil;

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

	private Map<String, Float> quote_(Streamlet<String> symbols, String dummy) {
		return queryFactors(symbols, false).toMap(factor -> factor.symbol, factor -> factor.quote);
	}

	// http://blog.sina.com.cn/s/blog_5dc29fcc0101dq5s.html
	public Streamlet<Factor> queryFactors(Streamlet<String> symbols, boolean isCache) {
		return 0 < symbols.size() ? queryFactor_(symbols, isCache) : Read.empty();
	}

	private Streamlet<Factor> queryFactor_(Streamlet<String> symbols, boolean isCache) {
		var url = "http://hq.sinajs.cn/?list=" + symbols //
				.map(this::toSina) //
				.collect(As.joinedBy(","));

		var data = rethrow(() -> {
			Outlet<Bytes> in;

			if (isCache)
				in = Singleton.me.storeCache.http(url);
			else
				in = HttpUtil.get(url).out();

			return in //
					.map(bytes -> {
						var sb = new StringBuilder();
						for (var i = 0; i < bytes.size(); i++)
							sb.append((char) bytes.get(i));
						return sb;
					}) //
					.collect(As::joined);
		});

		return Read //
				.from(data.split("\n")) //
				.map(line -> ParseUtil.fit(line, "var hq_str_", "=\"", "\"").map((t0, t1, t2) -> {

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

	private String toYahoo(String sina) {
		var prefix = "rt_hk0";
		return sina.startsWith(prefix) ? sina.substring(prefix.length()) + ".HK" : fail(sina);
	}

	private String toSina(String symbol_) {
		return "rt_hk0" + symbol_.substring(0, 4);
	}

}
