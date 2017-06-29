package suite.trade.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.trade.Time;

public class HkexUtilTest {

	@Test
	public void test() {
		Time on = Time.of("2017-06-29 12:48:00");
		Time off = Time.of("2017-06-29 22:48:00");

		assertEquals(Time.of("2017-06-29 12:48:00"), HkexUtil.getTradeTimeBefore(on));
		assertEquals(Time.of("2017-06-29 12:48:00"), HkexUtil.getTradeTimeAfter(on));
		assertEquals(Time.of("2017-06-29 09:00:00"), HkexUtil.getOpenTimeBefore(on));
		assertEquals(Time.of("2017-06-28 16:30:00"), HkexUtil.getCloseTimeBefore(on));

		assertEquals(Time.of("2017-06-29 16:29:59"), HkexUtil.getTradeTimeBefore(off));
		assertEquals(Time.of("2017-06-30 09:00:00"), HkexUtil.getTradeTimeAfter(off));
		assertEquals(Time.of("2017-06-29 09:00:00"), HkexUtil.getOpenTimeBefore(off));
		assertEquals(Time.of("2017-06-29 16:30:00"), HkexUtil.getCloseTimeBefore(off));
	}

}
