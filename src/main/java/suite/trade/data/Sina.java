package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.util.ParseUtil;
import suite.util.Rethrow;
import suite.util.To;

public class Sina {

	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	public class Factor {
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
		return queryFactor(symbol, true);
	}

	private Map<String, Float> quote_(Streamlet<String> symbols, String dummy) {
		return symbols //
				.map2(symbol -> queryFactor(symbol, false).quote) //
				.toMap();
	}

	private Factor queryFactor(String symbol, boolean isCache) {
		String urlString = "http://hq.sinajs.cn/?_=&list=rt_hk0" + symbol.substring(0, 4);

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

		String[] array = ParseUtil.fit(data, "\"", "\"")[1].split(",");

		// HSBC HOLDINGS,XXX,
		// 73.250,73.350,73.700,73.050, 73.150,
		// -0.200,-0.273,
		// 73.150,73.200,
		// 1439148052.782,19650107,
		// 77.098,0.697,
		// 74.500,46.350,
		// 2017/07/07,16:08:44,
		// 100|0,N|Y|Y,73.200|69.600|75.450,0|||0.000|0.000|0.000, |0,Y

		Factor factor = new Factor();
		factor.name = array[0];
		factor.open = Float.parseFloat(array[2]);
		factor.close = Float.parseFloat(array[3]);
		factor.high = Float.parseFloat(array[4]);
		factor.low = Float.parseFloat(array[5]);
		factor.quote = Float.parseFloat(array[6]);
		factor.change = Float.parseFloat(array[7]);
		factor.changePercent = Float.parseFloat(array[8]);
		factor.bid = Float.parseFloat(array[9]);
		factor.ask = Float.parseFloat(array[10]);
		factor.volumeHkd = Float.parseFloat(array[11]);
		factor.volume = Float.parseFloat(array[12]); // in stock
		factor.pe = Float.parseFloat(array[13]);
		factor.weeklyInterest = Float.parseFloat(array[14]);
		factor.high52week = Float.parseFloat(array[15]);
		factor.low52week = Float.parseFloat(array[16]);
		factor.lastCloseDate = array[17]; // 2017/07/07
		factor.lastCloseTime = array[18]; // 16:08:44

		return factor;
	}

}
