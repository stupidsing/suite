package suite.trade.backalloc;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.DatePeriod;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Sink;

public class MovingAvgMeanReversionBackAllocatorTest {

	private Sink<String> log = System.out::println;
	private Configuration cfg = new ConfigurationImpl();
	private MovingAvgMeanReversionBackAllocator backAllocator = MovingAvgMeanReversionBackAllocator.of_(log);

	@Test
	public void testStat() {
		DatePeriod period = DatePeriod.backTestDaysBefore(LocalDate.now(), 512, 32);
		System.out.println(backAllocator.new MeanReversionStat(cfg.dataSource("1113.HK"), period));
	}

}
