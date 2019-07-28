package suite.trade.backalloc.strategy;

import static java.lang.Math.abs;

import suite.math.numeric.Statistic;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.ts.TimeSeries;
import suite.util.To;

public class ReverseCorrelateBackAllocator implements BackAllocator {

	private int tor;
	private double reduction;
	private double reverseCorrelationThreshold;

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocator of() {
		return of(48, 9d, .03d);
	}

	public static BackAllocator of(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		return new ReverseCorrelateBackAllocator(tor, kellyReduction, reverseCorrelationThreshold);
	}

	private ReverseCorrelateBackAllocator(int tor, double kellyReduction, double reverseCorrelationThreshold) {
		this.tor = tor;
		this.reduction = kellyReduction;
		this.reverseCorrelationThreshold = reverseCorrelationThreshold;
	}

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		var dsBySymbol = akds.dsByKey;

		var dsv = DataSourceView.of(0, 512, akds, (symbol, ds, samplePeriod) -> {
			var prices = ds.range(samplePeriod).prices;
			var logReturns = ts.logReturns(prices);
			var ll = logReturns.length;
			var sum = 0d;
			for (var i = tor; i < ll - tor; i++) {
				var i_ = i;
				sum += stat.correlation(j -> logReturns[i_ - j], j -> logReturns[i_ + j], tor);
			}
			return sum / (ll - 2 * tor);
		});

		return index -> {
			var reverseCorrelationBySymbol = dsBySymbol //
					.map2((symbol, ds) -> {
						Double reverseCorrelation = dsv.get(symbol, index);
						return reverseCorrelation != null ? reverseCorrelation : Double.NaN;
					}) //
					.filterValue(Double::isFinite) //
					.filterValue(reverseCorrelation -> reverseCorrelationThreshold < abs(reverseCorrelation)) //
					.toMap();

			var reversePricesBySymbol = dsBySymbol //
					.filterKey(reverseCorrelationBySymbol::containsKey) //
					.mapValue(ds -> {
						var prices = ds.prices;
						var last = index - 1;
						return To.vector(tor, i -> prices[last - i]);
					}) //
					.collect();

			return new KellyCriterion().allocate(reversePricesBySymbol, reduction);
		};
	}

}
