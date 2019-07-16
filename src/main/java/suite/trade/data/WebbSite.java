package suite.trade.data;

import static java.util.Map.entry;

import java.util.Map;

import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.trade.Time;
import suite.trade.data.DataSource.Datum;
import suite.util.String_;

// https://www.facebook.com/notes/yuen-cheng/%E5%88%86%E4%BA%AB%E6%8B%BF%E5%8F%96webb-site%E6%B8%AF%E8%82%A1%E8%82%A1%E5%83%B9%E7%A8%8B%E5%BC%8F/1642684929197453/
public class WebbSite {

	@SuppressWarnings("unused")
	public class Factor {
		private Time atDate;
		private Time settleDate;
		private boolean susp;
		private float closing;
		private float bid;
		private float ask;
		private float low;
		private float high;
		private float vol;
		private float turn;
		private float VWAP;
		private float adjClose;
		private float adjBid;
		private float adjAsk;
		private float adjLow;
		private float adjHigh;
		private float adjVol;
		private float adjVwap;
		private float totalReturn;
	}

	public DataSource dataSource(String symbol) {
		var urlString = "https://webb-site.com/dbpub/pricesCSV.asp?i=" + codeBySymbol.get(symbol);

		// atDate,settleDate,susp,closing,bid,ask,low,high,vol,turn,VWAP,adjClose,adjBid,adjAsk,adjLow,adjHigh,adjVol,adjVWAP,totalRet
		var data = Singleton.me.storeCache //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.map(vs -> {
					var factor = new Factor();
					factor.atDate = Time.of(vs[0]);
					factor.settleDate = Time.of(vs[1]);
					factor.susp = String_.equals(vs[2], "1");
					factor.closing = Float.parseFloat(vs[3]);
					factor.bid = Float.parseFloat(vs[4]);
					factor.ask = Float.parseFloat(vs[5]);
					factor.low = Float.parseFloat(vs[6]);
					factor.high = Float.parseFloat(vs[7]);
					factor.vol = Float.parseFloat(vs[8]);
					factor.turn = Float.parseFloat(vs[9]);
					factor.VWAP = Float.parseFloat(vs[10]);
					factor.adjClose = Float.parseFloat(vs[11]);
					factor.adjBid = Float.parseFloat(vs[12]);
					factor.adjAsk = Float.parseFloat(vs[13]);
					factor.adjLow = Float.parseFloat(vs[14]);
					factor.adjHigh = Float.parseFloat(vs[15]);
					factor.adjVol = Float.parseFloat(vs[16]);
					factor.adjVwap = Float.parseFloat(vs[17]);
					factor.totalReturn = Float.parseFloat(vs[18]);
					return factor;
				}) //
				.map(factor -> new Datum( //
						factor.atDate.addHours(9).epochSec(), //
						factor.atDate.addHours(16).addSeconds(30 * 60).epochSec(), //
						// (factor.bid + factor.ask) / 2f, //
						// factor.closing, //
						// factor.low, //
						// factor.high, //
						(factor.adjBid + factor.adjAsk) / 2f, //
						factor.adjClose, //
						factor.adjLow, //
						factor.adjHigh, //
						factor.vol)) //
				.collect();

		return DataSource.of(data);
	}

	private Map<String, String> codeBySymbol = Map.ofEntries( //
			entry("0001.HK", "44"), //
			entry("0002.HK", "63"), //
			entry("0003.HK", "348"), //
			entry("0005.HK", "1088"), //
			entry("0006.HK", "43"), //
			entry("0011.HK", "14"), //
			entry("0012.HK", "182"), //
			entry("0016.HK", "9"), //
			entry("0017.HK", "3"), //
			entry("0019.HK", "24"), //
			entry("0027.HK", "1112"), //
			entry("0066.HK", "2638"), //
			entry("0083.HK", "39"), //
			entry("0101.HK", "59"), //
			entry("0151.HK", "4973"), //
			entry("0175.HK", "70"), //
			entry("0267.HK", "51"), //
			entry("0270.HK", "113"), //
			entry("0288.HK", "12694"), //
			entry("0291.HK", "349"), //
			entry("0384.HK", "1100"), //
			entry("0386.HK", "2651"), //
			entry("0388.HK", "2516"), //
			entry("0390.HK", "4935"), //
			entry("0489.HK", "4675"), //
			entry("0669.HK", "322"), //
			entry("0688.HK", "293"), //
			entry("0700.HK", "3601"), //
			entry("0728.HK", "3310"), //
			entry("0762.HK", "2619"), //
			entry("0788.HK", "26715"), //
			entry("0823.HK", "4685"), //
			entry("0857.HK", "2755"), //
			entry("0883.HK", "2942"), //
			entry("0914.HK", "946"), //
			entry("0939.HK", "4661"), //
			entry("0941.HK", "998"), //
			entry("0960.HK", "5480"), //
			entry("0998.HK", "4846"), //
			entry("1038.HK", "49"), //
			entry("1044.HK", "1893"), //
			entry("1088.HK", "4595"), //
			entry("1093.HK", "994"), //
			entry("1099.HK", "5221"), //
			entry("1109.HK", "995"), //
			entry("1113.HK", "13537"), //
			entry("1177.HK", "2821"), //
			entry("1211.HK", "3452"), //
			entry("1288.HK", "6068"), //
			entry("1299.HK", "6325"), //
			entry("1336.HK", "7240"), //
			entry("1339.HK", "10165"), //
			entry("1359.HK", "2653"), //
			entry("1398.HK", "4784"), //
			entry("1658.HK", "21218"), //
			entry("1766.HK", "5030"), //
			entry("1800.HK", "4794"), //
			entry("1928.HK", "5503"), //
			entry("1988.HK", "5487"), //
			entry("1997.HK", "25421"), //
			entry("2007.HK", "4841"), //
			entry("2007.HK", "4841"), //
			entry("2018.HK", "4613"), //
			entry("2020.HK", "4885"), //
			entry("2202.HK", "6124"), //
			entry("2238.HK", "6044"), //
			entry("2313.HK", "4670"), //
			entry("2318.HK", "3606"), //
			entry("2319.HK", "3597"), //
			entry("2328.HK", "3327"), //
			entry("2382.HK", "4873"), //
			entry("2388.HK", "3299"), //
			entry("2601.HK", "5549"), //
			entry("2628.HK", "3412"), //
			entry("2688.HK", "2975"), //
			entry("2799.HK", "16482"), //
			entry("2800.HK", "5295"), //
			entry("2828.HK", "5290"), //
			entry("3323.HK", "4711"), //
			entry("3328.HK", "4596"), //
			entry("3968.HK", "4766"), //
			entry("3988.HK", "4744"), //
			entry("6030.HK", "6378"), //
			entry("6837.HK", "5491"), //
			entry("6886.HK", "11014"));

}
