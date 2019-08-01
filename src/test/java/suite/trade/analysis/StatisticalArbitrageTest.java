package suite.trade.analysis;

import static java.lang.Math.abs;
import static suite.util.Streamlet_.forInt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import primal.String_;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.os.Log_;
import primal.primitive.Int_Flt;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.adt.pair.IntFltPair;
import suite.algo.KmeansCluster;
import suite.math.numeric.Statistic;
import suite.math.transform.DiscreteCosineTransform;
import suite.primitive.AsDbl;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.Sina;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.BollingerBands;
import suite.ts.Quant;
import suite.ts.TimeSeries;
import suite.util.To;

public class StatisticalArbitrageTest {

	private TimeRange period = TimeRange.threeYears();

	private BollingerBands bb = new BollingerBands();
	private TradeCfg cfg = new TradeCfgImpl();
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

		var ds = cfg.dataSource(Instrument.hsiSymbol).cleanse();
		var prices = ds.prices;
		var mas = To.array(power, float[].class, p -> ma.movingAvg(prices, 1 << p));
		var returns = ts.returns(prices);

		var lr = stat.linearRegression(forInt(1 << power, prices.length)
				.map(i -> FltObjPair.of(returns[i], To.vector(power, p -> mas[p][i - (1 << p)]))));

		System.out.println(lr);
	}

	// Auto-regressive test
	@Test
	public void testCointegration() {

		// 0004.HK, 0020.HK
		// 0011.HK, 0005.HK
		var tor = 8;
		var symbol0 = "0011.HK";
		var symbol1 = "0005.HK";

		var akds = cfg.dataSources(period, Read.each(symbol0, symbol1));
		var pricesBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();

		var length = akds.ts.length;
		var prices0 = pricesBySymbol.get(symbol0);
		var prices1 = pricesBySymbol.get(symbol1);

		var lr = stat.linearRegression(
				forInt(tor, length).map(i -> FltObjPair.of(prices1[i], To.vector(tor, j -> prices0[i + j - tor]))));

		System.out.println(lr);
	}

	@Test
	public void testHurstExponent() {
		System.out.println(showStats(ds -> ts.hurst(ds.prices, 64)));
	}

	@Test
	public void testKMeansCluster() {
		var akds = dataSources();
		var returnsBySymbol = akds.dsByKey.mapValue(DataSource::returns).toMap();
		System.out.println(kmc(akds.ts.length, returnsBySymbol));
	}

	@Test
	public void testKMeansClusterDct() {
		var dctDataSource = dctDataSources();
		System.out.println(kmc(dctDataSource.length, dctDataSource.dctByKey.toMap()));
	}

	private String kmc(int length, Map<String, float[]> ptBySymbol) {
		return new KmeansCluster(length).result(ptBySymbol, 9, 300);
	}

	@Test
	public void testMarketDirection() {
		var ds = cfg.dataSource(Instrument.hsiSymbol).cleanse();
		var flagsArray = mt.time(ds.prices);
		var flags0 = "-----";

		for (var i = 0; i < ds.ts.length; i++) {
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

		var ds = cfg.dataSource(Instrument.hsiSymbol).range(period).cleanse();
		var returns = ds.returns();

		for (var bet = 0f - 2f; bet < 1f + 2f; bet += .02f) {
			var notBet = 1f - bet;
			var sum = 0d;

			for (var i = 0; i < nTrials; i++) {
				var account = 1d;
				for (var j = 0; j < nBets; j++) {
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
				.map(instrument -> instrument.symbol) //
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
		var dctDataSources = dctDataSources();

		for (var e : dctDataSources.dctByKey) {
			var dct = e.v;
			var max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

			for (var i = minPeriod; i < dct.length; i++) {
				var f = abs(dct[i]);
				if (max.t1 < f)
					max = IntFltPair.of(i, f);
			}

			Log_.info(e.k + " has period " + max.t0);
		}
	}

	// Naive Bayes return prediction
	@Test
	public void testReturnDistribution() {
		var prices = cfg.dataSource(Instrument.hsiSymbol).range(period).prices;
		var maxTor = 16;

		var differencesByTor = forInt(1, maxTor) //
				.mapIntObj(tor -> {
					var differences = ts.differences(tor, prices);
					Arrays.sort(differences);
					return differences;
				}) //
				.toMap();

		for (var tor = 1; tor < maxTor; tor++)
			System.out.println("tor = " + tor + ", " + stat.moments(differencesByTor.get(tor)));

		Int_Flt predictFun = t -> {
			var cpsArray = forInt(1, maxTor) //
					.map(tor -> {
						var differences = differencesByTor.get(tor);
						var length = differences.length;

						// cumulative probabilities
						var cps = new double[11];

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

			var probabilities = new HashMap<Double, Double>();

			for (int cpsi = 0, predDiff = -500; predDiff < 500; cpsi++, predDiff += 100) {
				var cpsi_ = cpsi;

				var sum = forInt(1, maxTor) //
						.map(i -> i) //
						.toDouble(AsDbl.sum(tor -> {
							var probability = cpsArray[tor - 1][cpsi_ + 1] - cpsArray[tor - 1][cpsi_];
							return 1d / probability;
						}));

				probabilities.put(predDiff + 100d / 2d, sum);
			}

			return Read //
					.from2(probabilities) //
					.sortByValue((p0, p1) -> Double.compare(p1, p0)) //
					.first().k.floatValue();
		};

		for (var t = maxTor + 1; t < prices.length; t++) {
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
			var bandwidths0 = bb.bb(ds.prices, 32, 0, 2f).bandwidths;
			var returns0 = ds.returns();
			var bandwidths1 = ts.drop(1, bandwidths0);
			var returns1 = ts.drop(1, returns0);
			return stat.project(bandwidths1, returns1);
		}));
	}

	private DctDataSource dctDataSources() {
		var akds = dataSources();
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
		var symbols = cfg //
				.queryCompaniesByMarketCap(Time.now()) //
				.map(instrument -> instrument.symbol);

		return cfg.dataSources(period, symbols);
	}

}
