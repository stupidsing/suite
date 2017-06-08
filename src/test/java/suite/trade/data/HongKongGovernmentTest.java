package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.trade.Time;

public class HongKongGovernmentTest {

	private HongKongGovernment hkg = new HongKongGovernment();

	@Test
	public void test() {
		List<Time> publicHolidays = hkg.queryPublicHolidays();
		System.out.println(publicHolidays);
		assertTrue(publicHolidays.contains(Time.of(2018, 12, 25)));
	}
}
