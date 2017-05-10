package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.util.To;

/**
 * Strategy based on ordinary least square linear regression.
 *
 * @author ywsing
 */
public class Ols3AssetAllocator implements AssetAllocator {

	private int tor = 16;

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceBySymbol, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					int length = prices.length;
					float[][] x = new float[length - tor][];
					for (int i = tor; i < length; i++)
						x[i - tor] = inputs(prices, i);
					float[] y = Arrays.copyOfRange(prices, tor, length);
					LinearRegression lr = stat.linearRegression(x, y);
					float pricex = lr.predict(inputs(prices, length));
					return pricex / dataSource.last().price - 1d;
				}) //
				.filterValue(potential -> 0d < potential) //
				.toList();
	}

	private float[] inputs(float[] prices, int i) {
		float[] powers0 = new float[] { 1f, };
		float[] powers1 = Arrays.copyOfRange(prices, i - tor, i);
		float[] powers2 = To.arrayOfFloats(powers1, x_ -> x_ * x_);
		float[] powers3 = To.arrayOfFloats(powers1.length, i_ -> powers1[i_] * powers2[i_]);
		return mtx.concat(powers0, powers1, powers2, powers3);
	}

}
