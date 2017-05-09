package suite.trade.assetalloc;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.DatePeriod;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Sink;

public class MovingAvgMeanReversionAssetAllocatorTest {

	private Sink<String> log = System.out::println;
	private Configuration configuration = new Configuration();
	private MovingAvgMeanReversionAssetAllocator assetAllocator = new MovingAvgMeanReversionAssetAllocator(configuration, log);

	@Test
	public void testStats() {
		DatePeriod period = DatePeriod.backTestDaysBefore(LocalDate.now(), 512, 32);
		System.out.println(assetAllocator.new MeanReversionStats(configuration.dataSource("1113.HK"), period));
	}

}
