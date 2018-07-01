package suite.trade.data;

import suite.Defaults;
import suite.node.util.Singleton;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.streamlet.As;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.String_;

public class Quandl {

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		var m = Defaults.secrets("quandl .0");
		var usMarketClose = 16l;
		var ph0 = period.to.epochSec() - (usMarketClose + 4) * 24 * 3600;
		var ph1 = ph0 - (ph0 % 86400l);

		var urlString = "https://www.quandl.com/api/v1/datasets/CHRIS/CME_CL1.csv" //
				+ "?ph=" + ph1 //
				+ (m != null ? "&api_key=" + m[0] : "");

		// Date, Open, High, Low, Last, Change, Settle, Volume, Previous Day
		// Open Interest
		var arrays = Singleton.me.storeCache //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> String_.compare(a0[0], a1[0])) //
				.collect();

		var ts = arrays.collect(Obj_Lng.lift(array -> Time.of(array[0] + " 18:00:00").epochSec(-4))).toArray();
		var opens = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))).toArray();
		var settles = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[6]))).toArray();
		var lows = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[3]))).toArray();
		var highs = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[2]))).toArray();
		var volumes = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[7]))).toArray();

		return DataSource.ofOhlcv(ts, opens, settles, lows, highs, volumes).range(period);
	}

}
