package suite.trade.assetalloc;

import java.util.Map;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.To;

public class ReverseCorrelateAssetAllocator implements AssetAllocator {

	private int tor;
	private double kellyReduction;
	private double reverseCorrelationThreshold;

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static AssetAllocator of() {
		return of(48, 9d, .03d);
	}

	public static AssetAllocator of(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		return AssetAllocator_.unleverage(new ReverseCorrelateAssetAllocator(tor, kellyReduction, reverseCorrelationThreshold));
	}

	private ReverseCorrelateAssetAllocator(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		this.tor = tor;
		this.kellyReduction = kellyReduction;
		this.reverseCorrelationThreshold = reverseCorrelationThreshold;
	}

	@Override
	public OnDate allocate(Streamlet2<String, DataSource> dataSourceBySymbol) {
		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		return (backTestDate, index) -> {
			DatePeriod samplePeriod = DatePeriod.backTestDaysBefore(backTestDate, 512, 32);

			Map<String, Double> reverseCorrelationBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> {
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

			return Read.range(nSymbols) //
					.map2(i -> symbols[i], i -> allocations[i] * kellyReduction) //
					.toList();
		};
	}

}
