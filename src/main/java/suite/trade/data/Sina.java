package suite.trade.data;

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
import suite.util.Rethrow;
import suite.util.To;

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

	private Streamlet<Factor> queryFactors(Streamlet<String> symbols, boolean isCache) {
		String urlString = "http://hq.sinajs.cn/?list=" + symbols //
				.map(symbol_ -> "rt_hk0" + symbol_.substring(0, 4)) //
				.collect(As.joined(","));

		String data = Rethrow.ex(() -> {
			Outlet<Bytes> in;

			if (isCache)
				in = Singleton.me.storeCache.http(urlString);
			else
				in = HttpUtil.get(To.url(urlString)).out;

			return in //
					.map(bytes -> {
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < bytes.size(); i++)
							sb.append((char) bytes.get(i));
						return sb.toString();
					}) //
					.collect(As.joined());
		});

		return Read //
				.from(data.split("\n")) //
				.map(line -> {
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

					String[] a0 = ParseUtil.fit(line, "var hq_str_rt_hk0", "=\"", "\"");
					String[] a1 = a0[2].split(",");

					Factor factor = new Factor();
					factor.symbol = a0[1] + ".HK";
					factor.name = a1[0];
					factor.open = Float.parseFloat(a1[2]);
					factor.close = Float.parseFloat(a1[3]);
					factor.high = Float.parseFloat(a1[4]);
					factor.low = Float.parseFloat(a1[5]);
					factor.quote = Float.parseFloat(a1[6]);
					factor.change = Float.parseFloat(a1[7]);
					factor.changePercent = Float.parseFloat(a1[8]);
					factor.bid = Float.parseFloat(a1[9]);
					factor.ask = Float.parseFloat(a1[10]);
					factor.volumeHkd = Float.parseFloat(a1[11]);
					factor.volume = Float.parseFloat(a1[12]); // in stock
					factor.pe = Float.parseFloat(a1[13]);
					factor.weeklyInterest = Float.parseFloat(a1[14]);
					factor.high52week = Float.parseFloat(a1[15]);
					factor.low52week = Float.parseFloat(a1[16]);
					factor.lastCloseDate = a1[17]; // 2017/07/07
					factor.lastCloseTime = a1[18]; // 16:08:44
					return factor;
				}) //
				.collect(As::streamlet);
	}

}
