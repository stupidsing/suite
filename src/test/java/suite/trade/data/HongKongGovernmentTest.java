package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

public class HongKongGovernmentTest {

	private HongKongGovernment hkg = new HongKongGovernment();

	@Test
	public void test() {
		List<LocalDate> publicHolidays = hkg.queryPublicHolidays();
		System.out.println(publicHolidays);
		assertTrue(publicHolidays.contains(LocalDate.of(2018, 12, 25)));
	}
}
