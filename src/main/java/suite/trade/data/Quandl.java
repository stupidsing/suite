package suite.trade.data;

import java.util.List;

import suite.Constants;
import suite.node.util.Singleton;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.String_;

public class Quandl {

	public DataSource dataSourceCsv(String symbol, TimeRange period0) {
		String[] m = Constants.secrets("quandl .0");
		long usMarketClose = 16l;
		long ph0 = period0.to.epochSec() - (usMarketClose + 4) * 24 * 3600;
		long ph1 = ph0 - (ph0 % 86400l);

		String urlString = "https://www.quandl.com/api/v1/datasets/CHRIS/CME_CL1.csv" //
				+ "?ph=" + ph1 //
				+ (m != null ? "&api_key=" + m[0] : "");

		// Date, Open, High, Low, Last, Change, Settle, Volume, Previous Day
		// Open Interest
		List<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> String_.compare(a0[0], a1[0])) //
				.toList();

		long[] ts = Read.from(arrays) //
				.collect(Obj_Lng.lift(array -> Time.of(array[0] + " 18:00:00").epochSec(-4))) //
				.toArray();

		float[] prices = Read.from(arrays) //
				.collect(Obj_Flt.lift(array -> Float.parseFloat(array[4]))) //
				.toArray();

		return new DataSource(ts, prices).range(period0);
	}

}
