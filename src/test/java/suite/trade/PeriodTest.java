package suite.trade;

import java.util.Arrays;

import org.junit.Test;

import suite.math.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.trade.Hkex.Company;

/**
 * Finds the period of various stocks using FFT.
 *
 * @author ywsing
 */
public class PeriodTest {

	private Hkex hkex = new Hkex();

	private int minPeriod = 8;

	@Test
	public void test() {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();

		for (Company stock : hkex.companies.take(40)) {
			String stockCode = stock.code + ".HK";
			String disp = stock.toString();

			try {
				DataSource ds = DataSource.yahoo(stockCode);
				float[] prices0 = ds.prices;
				int size = 1, size1;

				while ((size1 = size << 1) <= prices0.length)
					size = size1;

				float[] prices1 = Arrays.copyOf(prices0, size);
				float[] fs = dct.dct(prices1);
				int maxIndex = minPeriod;
				float maxValue = fs[minPeriod];

				for (int i = minPeriod; i < size; i++) {
					float f = Math.abs(fs[i]);
					if (maxValue < f) {
						maxIndex = i;
						maxValue = f;
					}
				}

				LogUtil.info(disp + " has period " + maxIndex);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + disp);
			}
		}
	}

}
