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
import suite.util.Object_;

public class Quandl {

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		String[] m = Constants.secrets("quandl .0");

		String urlString = "https://www.quandl.com/api/v1/datasets/CHRIS/CME_CL1.csv" //
				+ "?ph=" + period.hashCode() //
				+ (m != null ? "&api_key=" + m[0] : "");

		// Date, Open, High, Low, Last, Change, Settle, Volume, Previous Day
		// Open Interest
		List<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> Object_.compare(a0[0], a1[0])) //
				.toList();

		long[] ts = Read.from(arrays) //
				.collect(Obj_Lng.lift(array -> Time.of(array[0]).epochSec())) //
				.toArray();

		float[] prices = Read.from(arrays) //
				.collect(Obj_Flt.lift(array -> Float.parseFloat(array[4]))) //
				.toArray();

		return new DataSource(ts, prices).range(period);
	}

}
