package suite.trade.data;

import java.util.Arrays;

import org.junit.Test;

import suite.trade.Time;
import suite.trade.TimeRange;

public class QuandlTest {

	private Quandl quandl = new Quandl();

	@Test
	public void test() {
		var e = Time.now();
		var s = e.addDays(-360);
		var pair = quandl.dataSourceCsvV3("CHRIS/CME_CL1", TimeRange.of(s, e)); // "WGFD/WLD_GFDD_DI_13"
		System.out.println(Arrays.toString(pair.k));
		System.out.println(Arrays.toString(pair.v.get(0)));
	}

}
