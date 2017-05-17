package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.algo.Statistic;
import suite.math.CholeskyDecomposition;
import suite.math.TimeSeries;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.util.To;

public class KellyAssetAllocator implements AssetAllocator {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public OnDate allocate(Map<String, DataSource> dataSourceBySymbol, List<LocalDate> tradeDates) {
		return backTestDate -> {

			// TODO this should be the expected returns, not past returns!
			Map<String, DataSource> predictedPricesBySymbol = Read.from2(dataSourceBySymbol) //
					.mapValue(dataSource -> dataSource.rangeBefore(backTestDate)) //
					.toMap();

			Map<String, float[]> returnsBySymbol = Read.from2(predictedPricesBySymbol) //
					.mapValue(dataSource -> ts.returns(dataSource.prices)) //
					.toMap();

			Map<String, Double> excessReturnBySymbol = Read.from2(predictedPricesBySymbol) //
					.mapValue(dataSource -> {
						double price0 = dataSource.first().price;
						double pricex = dataSource.last().price;
						double nYears = dataSource.nYears();
						int nDays = dataSource.dates.length;
						double dailyInterestRate = Math.exp(stat.logRiskFreeInterestRate * nYears / nDays);
						return pricex / price0 - dailyInterestRate;
					}) //
					.toMap();

			String[] symbols = returnsBySymbol.keySet().toArray(new String[0]);
			int nSymbols = symbols.length;

			float[][] cov = To.arrayOfFloats(nSymbols, nSymbols, (i0, i1) -> {
				float[] returns0 = returnsBySymbol.get(symbols[i0]);
				float[] returns1 = returnsBySymbol.get(symbols[i1]);
				return (float) stat.covariance(returns0, returns1);
			});

			float[] returns = To.arrayOfFloats(symbols, symbol -> excessReturnBySymbol.get(symbol).floatValue());

			float[] allocations = cholesky.inverseMul(cov).apply(returns);

			return Read.range(nSymbols) //
					.map2(i -> symbols[i], i -> (double) allocations[i]) //
					.filterValue(potential -> 0d < potential) //
					.toList();
		};
	}

}
