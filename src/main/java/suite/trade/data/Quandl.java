package suite.trade.data;

import java.util.List;

import primal.Verbs.Compare;
import primal.adt.Pair;
import suite.cfg.Defaults;
import suite.node.util.Singleton;
import suite.primitive.AsFlt;
import suite.primitive.AsLng;
import suite.primitive.adt.map.ObjIntMap;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;

public class Quandl {

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		var usMarketClose = 16l;
		var ph0 = period.to.epochSec() - (usMarketClose + 4) * 24 * 3600;
		var ph1 = ph0 - (ph0 % 86400l);

		var urlString = "https://www.quandl.com/api/v1/datasets/CHRIS/CME_CL1.csv?ph=" + ph1;

		// Date, Open, High, Low, Last, Change, Settle, Volume, Previous Day
		// Open Interest
		return csv(urlString).map((headers, csv) -> {
			var arrays = csv(urlString).v //
					.skip(1) //
					.sort((a0, a1) -> Compare.string(a0[0], a1[0])) //
					.collect();

			var ts = arrays.collect(AsLng.lift(array -> Time.of(array[0] + " 18:00:00").epochSec(-4))).toArray();
			var opens = arrays.collect(AsFlt.lift(array -> Float.parseFloat(array[1]))).toArray();
			var settles = arrays.collect(AsFlt.lift(array -> Float.parseFloat(array[6]))).toArray();
			var lows = arrays.collect(AsFlt.lift(array -> Float.parseFloat(array[3]))).toArray();
			var highs = arrays.collect(AsFlt.lift(array -> Float.parseFloat(array[2]))).toArray();
			var volumes = arrays.collect(AsFlt.lift(array -> Float.parseFloat(array[7]))).toArray();

			return DataSource.ofOhlcv(ts, opens, settles, lows, highs, volumes).range(period);
		});
	}

	public Pair<String[], List<String[]>> dataSourceCsvV3(String qn, TimeRange period) {
		var urlString = "https://www.quandl.com/api/v3/datasets/" + qn + ".csv" //
				+ "?start_date=" + period.fr.ymd() //
				+ "&end_date=" + period.to.ymd() //
				+ "&order=asc";
		// + "&collapse=annual"

		// Date, Value
		var csv = csv(urlString);
		var header = csv.v.first();
		var list = csv.v.skip(1).toList();
		return Pair.of(header, list);
	}

	private Pair<ObjIntMap<String>, Streamlet<String[]>> csv(String urlString) {
		var m = Defaults.secrets("quandl .0");

		return Singleton.me.storeCache.http(urlString + (m != null ? "&api_key=" + m[0] : "")).collect(As::csvWithHeader);
	}

}
