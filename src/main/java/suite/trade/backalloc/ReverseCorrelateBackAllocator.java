package suite.trade.backalloc;

import java.util.List;
import java.util.Map;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
import suite.streamlet.IntStreamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.To;

public class ReverseCorrelateBackAllocator implements BackAllocator {

	private int tor;
	private double kellyReduction;
	private double reverseCorrelationThreshold;

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocator of() {
		return of(48, 9d, .03d);
	}

	public static BackAllocator of(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		return new ReverseCorrelateBackAllocator(tor, kellyReduction, reverseCorrelationThreshold).unleverage();
	}

	private ReverseCorrelateBackAllocator(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		this.tor = tor;
		this.kellyReduction = kellyReduction;
		this.reverseCorrelationThreshold = reverseCorrelationThreshold;
	}

	@Override
	public OnDateTime allocate(Streamlet2<String, DataSource> dataSourceBySymbol, List<Time> times) {
		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		Map<String, Map<TimeRange, Double>> reverseCorrelationByPeriodBySymbol = dataSourceBySymbol //
				.mapValue(dataSource -> TimeRange //
						.ofDateTimes(times) //
						.backTestDaysBefore(512, 32) //
						.map2(samplePeriod -> {
							float[] prices = dataSource.range(samplePeriod).prices;
							float[] logReturns = ts.logReturns(prices);
							int ll = logReturns.length;
							double sum = 0d;
							for (int i = tor; i < ll - tor; i++) {
								int i_ = i;
								sum += stat.correlation(j -> logReturns[i_ - j], j -> logReturns[i_ + j], tor);
							}
							return sum / (ll - 2 * tor);
						}) //
						.toMap()) //
				.toMap();

		return (time, index) -> {
			TimeRange samplePeriod = TimeRange.backTestDaysBefore(time, 512, 32);

			Map<String, Double> reverseCorrelationBySymbol = dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						Map<TimeRange, Double> m = reverseCorrelationByPeriodBySymbol.get(symbol);
						Double reverseCorrelation = m != null ? m.get(samplePeriod) : null;
						return reverseCorrelation != null ? reverseCorrelation : Double.NaN;
					}) //
					.filterValue(Double::isFinite) //
					.filterValue(reverseCorrelation -> reverseCorrelationThreshold < Math.abs(reverseCorrelation)) //
					.toMap();

			Streamlet2<String, float[]> reversePricesBySymbol = dataSourceBySymbol //
					.filterKey(reverseCorrelationBySymbol::containsKey) //
					.mapValue(dataSource -> {
						float[] prices = dataSource.prices;
						int last = index - 1;
						return To.arrayOfFloats(tor, i -> prices[last - i]);
					}) //
					.collect(As::streamlet2);

			Map<String, float[]> returnsBySymbol = reversePricesBySymbol //
					.mapValue(ts::returns) //
					.toMap();

			Map<String, Float> excessReturnBySymbol = reversePricesBySymbol //
					.map2((symbol, prices) -> {
						double reverseCorrelation = reverseCorrelationBySymbol.get(symbol);
						double price0 = prices[0];
						double priceDiff = prices[tor - 1] - price0;
						double returnTor = priceDiff / price0;
						double returnDaily = Math.expm1(Math.log1p(returnTor) / tor) * Math.signum(reverseCorrelation);
						return (float) (returnDaily - dailyRiskFreeInterestRate);
					}) //
					.toMap();

			String[] symbols = returnsBySymbol.keySet().toArray(new String[0]);
			int nSymbols = symbols.length;

			float[][] cov = To.arrayOfFloats(nSymbols, nSymbols, (i0, i1) -> {
				float[] returns0 = returnsBySymbol.get(symbols[i0]);
				float[] returns1 = returnsBySymbol.get(symbols[i1]);
				return (float) (stat.covariance(returns0, returns1) * tor);
			});

			float[] returns = To.arrayOfFloats(symbols, excessReturnBySymbol::get);
			float[] allocations = cholesky.inverseMul(cov).apply(returns);

			return IntStreamlet //
					.range(nSymbols) //
					.map2(i -> symbols[i], i -> allocations[i] * kellyReduction) //
					.toList();
		};
	}

}
