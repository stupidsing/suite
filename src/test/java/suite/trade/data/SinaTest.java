package suite.trade.data;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.inspect.Dump;
import suite.trade.data.Sina.Factor;

public class SinaTest {

	private Sina sina = new Sina();

	@Test
	public void test() {
		Factor factor = sina.queryFactor("0005.HK");
		assertNotNull(factor);
		Dump.out(factor);
	}

}
