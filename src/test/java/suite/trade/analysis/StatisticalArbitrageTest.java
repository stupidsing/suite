package suite.trade.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;
import suite.trade.TimeRange;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.util.Object_;
import suite.util.To;

public class StatisticalArbitrageTest {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	// Auto-regressive test
	@Test
	public void testCointegration() {
		int tor = 8;
		String symbol0 = "CLQ17.NYM";
		String symbol1 = "1055.HK";
		TimeRange period = TimeRange.threeYears();

		AlignKeyDataSource<String> akds = cfg.dataSources(period, Read.each(symbol0, symbol1));

		Map<String, float[]> pricesBySymbol = akds.dsByKey //
				.mapValue(ds -> ts.returns(ds.prices)) //
				.toMap();

		int length = akds.ts.length;
		float[] prices0 = pricesBySymbol.get(symbol0);
		float[] prices1 = pricesBySymbol.get(symbol1);

		float[][] xs = IntStreamlet //
				.range(tor, length) //
				.map(i -> To.arrayOfFloats(tor, j -> prices0[i + j - tor])) //
				.toArray(float[].class);
		float[] ys = IntStreamlet //
				.range(tor, length) //
				.collect(Int_Flt.lift(i -> prices1[i])) //
				.toArray();

		LinearRegression lr = stat.linearRegression(xs, ys);
		System.out.println(lr);
	}

	// Naive Bayes return prediction
	@Test
	public void testReturnDistribution() {
		TimeRange period = TimeRange.threeYears();
		float[] prices = cfg.dataSource("^HSI").range(period).prices;
		int maxTor = 16;

		IntObjMap<float[]> differencesByTor = IntStreamlet //
				.range(1, maxTor) //
				.mapIntObj(tor -> {
					float[] differences = ts.differences(tor, prices);
					Arrays.sort(differences);
					return differences;
				}) //
				.toMap();

		for (int tor = 1; tor < maxTor; tor++) {
			float[] differences = differencesByTor.get(tor);
			MeanVariance mv = stat.meanVariance(differences);

			System.out.println("tor = " + tor //
					+ ", mean = " + mv.mean //
					+ ", variance = " + mv.variance //
					+ ", skewness = " + stat.skewness(differences) //
					+ ", kurtosis = " + stat.kurtosis(differences));
		}

		Int_Flt predictFun = t -> {
			double[][] cpsArray = IntStreamlet //
					.range(1, maxTor) //
					.map(tor -> {
						float[] differences = differencesByTor.get(tor);
						int length = differences.length;

						// cumulative probabilities
						double[] cps = new double[11];

						for (int cpsi = 0, predDiff = -500; predDiff <= 500; cpsi++, predDiff += 100) {
							float f = prices[t - 1] + predDiff - prices[t - tor];
							int i = 0;
							while (i < length && differences[i] < f)
								i++;
							cps[cpsi] = i / (double) length;
						}

						return cps;
					}) //
					.toArray(double[].class);

			Map<Double, Double> probabilities = new HashMap<>();

			for (int cpsi = 0, predDiff = -500; predDiff < 500; cpsi++, predDiff += 100) {
				int cpsi_ = cpsi;

				double sum = IntStreamlet //
						.range(1, maxTor) //
						.map(i -> i) //
						.collectAsDouble(Obj_Dbl.sum(tor -> {
							double probability = cpsArray[tor - 1][cpsi_ + 1] - cpsArray[tor - 1][cpsi_];
							return 1d / probability;
						}));

				probabilities.put(predDiff + 100d / 2d, sum);
			}

			return Read.from2(probabilities) //
					.sortByValue((p0, p1) -> Object_.compare(p1, p0)) //
					.first().t0.floatValue();
		};

		for (int t = maxTor + 1; t < prices.length; t++) {
			float predicted = prices[t - 1] + predictFun.apply(t);

			System.out.println("t = " + t //
					+ ", actual = " + prices[t] //
					+ ", predicted = " + predicted);
		}
	}

}
