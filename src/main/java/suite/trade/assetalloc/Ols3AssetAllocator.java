package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.util.To;

/**
 * Strategy based on ordinary least square linear regression.
 *
 * @author ywsing
 */
public class Ols3AssetAllocator implements AssetAllocator {

	private int lookBack;

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	public static AssetAllocator of() {
		return of(16);
	}

	public static AssetAllocator of(int lookBack) {
		return AssetAllocator_.filterShorts(new Ols3AssetAllocator(lookBack));
	}

	private Ols3AssetAllocator(int lookBack) {
		this.lookBack = lookBack;
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate, int index) {
		return Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					float[][] x = new float[index - lookBack][];
					for (int i = lookBack; i < index; i++)
						x[i - lookBack] = inputs(prices, i);
					float[] y = Arrays.copyOfRange(prices, lookBack, index);
					LinearRegression lr = stat.linearRegression(x, y);
					float pricex = lr.predict(inputs(prices, index));
					return pricex / dataSource.prices[index] - 1d;
				}) //
				.toList();
	}

	private float[] inputs(float[] prices, int i) {
		float[] powers0 = new float[] { 1f, };
		float[] powers1 = Arrays.copyOfRange(prices, i - lookBack, i);
		float[] powers2 = To.arrayOfFloats(powers1, x_ -> x_ * x_);
		float[] powers3 = To.arrayOfFloats(powers1.length, i_ -> powers1[i_] * powers2[i_]);
		return mtx.concat(powers0, powers1, powers2, powers3);
	}

}
