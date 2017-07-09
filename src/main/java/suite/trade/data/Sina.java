package suite.trade.data;

import java.io.InputStream;

import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.ParseUtil;
import suite.util.Rethrow;
import suite.util.To;

public class Sina {

	public class Factor {
		public String name;
		public String open;
		public String close;
		public String high;
		public String low;
		public String quote;
		public String change;
		public String changePercent;
		public String volumeHkd;
		public String volume; // in stock
		public String pe;
		public String weeklyInterest;
		public String high52week;
		public String low52week;
		public String lastCloseDate; // 2017/07/07
		public String lastCloseTime; // 16:08:44
	}

	public Factor queryFactor(String symbol) {
		String urlString = "http://hq.sinajs.cn/?_=1499516355436&list=rt_hk0" + symbol.substring(0, 4);

		String data = Rethrow.ex(() -> {
			try (InputStream is = Singleton.get().getStoreCache().http(urlString).collect(To::inputStream)) {
				return Read //
						.bytes(is) //
						.map(bytes -> {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < bytes.size(); i++)
								sb.append((char) bytes.get(i));
							return sb.toString();
						}) //
						.collect(As.joined());
			}
		});

		String[] array = ParseUtil.fit(data, "\"", "\"")[1].split(",");

		Factor factor = new Factor();
		factor.name = array[0];
		factor.open = array[2];
		factor.close = array[3];
		factor.high = array[4];
		factor.low = array[5];
		factor.quote = array[6];
		factor.change = array[7];
		factor.changePercent = array[8];
		factor.volumeHkd = array[11];
		factor.volume = array[12]; // in stock
		factor.pe = array[13];
		factor.weeklyInterest = array[14];
		factor.high52week = array[15];
		factor.low52week = array[16];
		factor.lastCloseDate = array[17]; // 2017/07/07
		factor.lastCloseTime = array[18]; // 16:08:44
		return factor;
	}

}
