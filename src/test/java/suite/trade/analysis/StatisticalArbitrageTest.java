package suite.trade.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.util.Object_;
import suite.util.To;

public class StatisticalArbitrageTest {

	private TimeRange period = TimeRange.threeYears();

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testMonteCarloBestBet() {
		int nTrials = 1000;
		int nBets = 20;

		float[] returns = cfg.dataSource("0005.HK").returns();
		Random random = new Random();

		for (float bet = 0f - 2f; bet < 1f + 2f; bet += .01f) {
			float notBet = 1f - bet;
			double sum = 0d;

			for (int i = 0; i < nTrials; i++) {
				double account = 1d;
				for (int j = 0; j < nBets; j++) {
					double return_ = 1d + returns[random.nextInt(returns.length)];
					account = notBet * account + bet * account * (1d + return_);
				}
				sum += account;
			}

			System.out.println("bet = " + To.string(bet) + ", avg outcome = " + To.string(sum / nTrials));
		}
	}

	// Auto-regressive test
	@Test
	public void testCointegration() {

		// 0004.HK, 0020.HK
		// 0011.HK, 0005.HK
		int tor = 8;
		String symbol0 = "0004.HK";
		String symbol1 = "0945.HK";

		AlignKeyDataSource<String> akds = cfg.dataSources(period, Read.each(symbol0, symbol1));
		Map<String, float[]> pricesBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();

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

		for (int tor = 1; tor < maxTor; tor++)
			System.out.println("tor = " + tor + ", " + stat.moments(differencesByTor.get(tor)));

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

	// any relationship between returns and volatility?
	@Test
	public void testVolatility() {
		BollingerBands bb = new BollingerBands();

		Streamlet<String> symbols = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(asset -> asset.symbol);

		AlignKeyDataSource<String> akds = cfg.dataSources(period, symbols);

		List<Pair<String, Double>> volBySymbol = akds.dsByKey //
				.map2((symbol, ds) -> {
					float[] bandwidths0 = bb.bb(ds.prices, 32, 0, 2f).bandwidth;
					float[] returns0 = ds.returns();
					float[] bandwidths1 = ts.drop(1, bandwidths0);
					float[] returns1 = ts.drop(1, returns0);
					return stat.correlation(bandwidths1, returns1);
				}) //
				.sortByValue(Object_::compare) //
				.toList();

		volBySymbol.forEach(System.out::println);
	}

}
