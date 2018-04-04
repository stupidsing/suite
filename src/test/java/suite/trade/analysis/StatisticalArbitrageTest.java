package suite.trade.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.algo.KmeansCluster;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Floats_;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.adt.pair.IntFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.Sina;
import suite.util.FunUtil.Fun;
import suite.util.String_;
import suite.util.To;
import ts.BollingerBands;
import ts.Quant;
import ts.TimeSeries;

public class StatisticalArbitrageTest {

	private TimeRange period = TimeRange.threeYears();

	private BollingerBands bb = new BollingerBands();
	private Configuration cfg = new ConfigurationImpl();
	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private MarketTiming mt = new MarketTiming();
	private MovingAverage ma = new MovingAverage();
	private Random random = new Random();
	private Sina sina = new Sina();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testAutoRegressivePowersOfTwo() {
		var power = 6;

		DataSource ds = cfg.dataSource(Asset.hsiSymbol).cleanse();
		float[] prices = ds.prices;
		float[][] mas = To.array(power, float[].class, p -> ma.movingAvg(prices, 1 << p));
		float[] returns = ts.returns(prices);

		LinearRegression lr = stat.linearRegression(Ints_ //
				.range(1 << power, prices.length) //
				.map(i -> FltObjPair.of(returns[i], Floats_.toArray(power, p -> mas[p][i - (1 << p)]))));

		System.out.println(lr);
	}

	// Auto-regressive test
	@Test
	public void testCointegration() {

		// 0004.HK, 0020.HK
		// 0011.HK, 0005.HK
		var tor = 8;
		var symbol0 = "0004.HK";
		var symbol1 = "0945.HK";

		AlignKeyDataSource<String> akds = cfg.dataSources(period, Read.each(symbol0, symbol1));
		Map<String, float[]> pricesBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();

		var length = akds.ts.length;
		float[] prices0 = pricesBySymbol.get(symbol0);
		float[] prices1 = pricesBySymbol.get(symbol1);

		LinearRegression lr = stat.linearRegression(Ints_ //
				.range(tor, length) //
				.map(i -> FltObjPair.of(prices1[i], Floats_.toArray(tor, j -> prices0[i + j - tor]))));

		System.out.println(lr);
	}

	@Test
	public void testHurstExponent() {
		System.out.println(showStats(ds -> ts.hurst(ds.prices, 64)));
	}

	@Test
	public void testKMeansCluster() {
		AlignKeyDataSource<String> akds = dataSources();
		Map<String, float[]> returnsBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();
		System.out.println(kmc(akds.ts.length, returnsBySymbol));
	}

	@Test
	public void testKMeansClusterDct() {
		DctDataSource dctDataSource = dctDataSources();
		System.out.println(kmc(dctDataSource.length, dctDataSource.dctByKey.toMap()));
	}

	private String kmc(int length, Map<String, float[]> ptBySymbol) {
		return new KmeansCluster(length).result(ptBySymbol, 9, 300);
	}

	@Test
	public void testMarketDirection() {
		DataSource ds = cfg.dataSource(Asset.hsiSymbol).cleanse();
		int[] flagsArray = mt.time(ds.prices);
		var flags0 = "-----";

		for (int i = 0; i < ds.ts.length; i++) {
			var flags = String_ //
					.right("00000" + Integer.toBinaryString(flagsArray[i]), -5) //
					.replace('0', '-') //
					.replace('1', 'M');

			if (!String_.equals(flags0, flags))
				System.out.println(Time.ofEpochSec(ds.ts[i]).ymd() + " " + flags);

			flags0 = flags;
		}
	}

	@Test
	public void testMonteCarloBestBet() {
		var nTrials = 10000;
		var nBets = 40;

		DataSource ds = cfg.dataSource(Asset.hsiSymbol).range(period).cleanse();
		float[] returns = ds.returns();

		for (float bet = 0f - 2f; bet < 1f + 2f; bet += .02f) {
			var notBet = 1f - bet;
			var sum = 0d;

			for (int i = 0; i < nTrials; i++) {
				var account = 1d;
				for (int j = 0; j < nBets; j++) {
					var return_ = returns[random.nextInt(returns.length)];
					account = notBet * account + bet * account * (1d + return_);
				}
				sum += account;
			}

			System.out.println("bet = " + To.string(bet) + ", avg outcome = " + To.string(sum / nTrials));
		}
	}

	@Test
	public void testPeRatio() {
		var out = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(asset -> asset.symbol) //
				.collect(symbols -> sina.queryFactors(As.streamlet(symbols), true)) //
				.map2(factor -> factor.symbol, factor -> factor.pe) //
				.sortByValue(Float::compare) //
				.map((symbol, peRatio) -> Pair.of(symbol, peRatio).toString()) //
				.collect(As.joinedBy("\n"));
		System.out.println(out);
	}

	// find the period of various stocks using FFT
	@Test
	public void testPeriod() {
		var minPeriod = 4;
		DctDataSource dctDataSources = dctDataSources();

		for (Pair<String, float[]> e : dctDataSources.dctByKey) {
			float[] dct = e.t1;
			IntFltPair max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

			for (int i = minPeriod; i < dct.length; i++) {
				var f = Math.abs(dct[i]);
				if (max.t1 < f)
					max = IntFltPair.of(i, f);
			}

			LogUtil.info(e.t0 + " has period " + max.t0);
		}
	}

	// Naive Bayes return prediction
	@Test
	public void testReturnDistribution() {
		float[] prices = cfg.dataSource(Asset.hsiSymbol).range(period).prices;
		var maxTor = 16;

		IntObjMap<float[]> differencesByTor = Ints_ //
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
			double[][] cpsArray = Ints_ //
					.range(1, maxTor) //
					.map(tor -> {
						float[] differences = differencesByTor.get(tor);
						var length = differences.length;

						// cumulative probabilities
						double[] cps = new double[11];

						for (int cpsi = 0, predDiff = -500; predDiff <= 500; cpsi++, predDiff += 100) {
							var f = prices[t - 1] + predDiff - prices[t - tor];
							var i = 0;
							while (i < length && differences[i] < f)
								i++;
							cps[cpsi] = i / (double) length;
						}

						return cps;
					}) //
					.toArray(double[].class);

			Map<Double, Double> probabilities = new HashMap<>();

			for (int cpsi = 0, predDiff = -500; predDiff < 500; cpsi++, predDiff += 100) {
				var cpsi_ = cpsi;

				var sum = Ints_ //
						.range(1, maxTor) //
						.map(i -> i) //
						.toDouble(Obj_Dbl.sum(tor -> {
							var probability = cpsArray[tor - 1][cpsi_ + 1] - cpsArray[tor - 1][cpsi_];
							return 1d / probability;
						}));

				probabilities.put(predDiff + 100d / 2d, sum);
			}

			return Read.from2(probabilities) //
					.sortByValue((p0, p1) -> Double.compare(p1, p0)) //
					.first().t0.floatValue();
		};

		for (int t = maxTor + 1; t < prices.length; t++) {
			var predicted = prices[t - 1] + predictFun.apply(t);

			System.out.println("t = " + t //
					+ ", actual = " + prices[t] //
					+ ", predicted = " + predicted);
		}
	}

	@Test
	public void testVarianceRatio() {
		System.out.println(showStats(ds -> ts.varianceRatio(ds.prices, 64)));
	}

	// any relationship between returns and volatility?
	@Test
	public void testVolatility() {
		System.out.println(showStats(ds -> {
			float[] bandwidths0 = bb.bb(ds.prices, 32, 0, 2f).bandwidths;
			float[] returns0 = ds.returns();
			float[] bandwidths1 = ts.drop(1, bandwidths0);
			float[] returns1 = ts.drop(1, returns0);
			return stat.project(bandwidths1, returns1);
		}));
	}

	private DctDataSource dctDataSources() {
		AlignKeyDataSource<String> akds = dataSources();
		var length0 = akds.ts.length;
		var log2 = Quant.log2trunc(length0);
		var fr = length0 - log2;
		return new DctDataSource(log2, akds.dsByKey.mapValue(ds -> dct.dct(Arrays.copyOfRange(ds.prices, fr, length0))));
	}

	private class DctDataSource {
		private int length;
		private Streamlet2<String, float[]> dctByKey;

		private DctDataSource(int t0, Streamlet2<String, float[]> t1) {
			this.length = t0;
			this.dctByKey = t1;
		}
	}

	private String showStats(Fun<DataSource, Double> fun) {
		return dataSources().dsByKey //
				.mapValue(fun) //
				.sortByValue(Double::compare) //
				.map((symbol, value) -> Pair.of(symbol, value).toString()) //
				.collect(As.joinedBy("\n"));
	}

	private AlignKeyDataSource<String> dataSources() {
		Streamlet<String> symbols = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(asset -> asset.symbol);

		return cfg.dataSources(period, symbols);
	}

}
