package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.math.CholeskyDecomposition;
import suite.math.TimeSeries;
import suite.streamlet.Read;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.To;

public class KellyAssetAllocator implements AssetAllocator {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static AssetAllocator of() {
		return AssetAllocator_.removeShorts(new KellyAssetAllocator());
	}

	private KellyAssetAllocator() {
	}

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceBySymbol, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		double dailyInterestRate = Trade_.riskFreeInterestRate(1);

		// TODO this should be the expected returns, not past returns!
		Map<String, float[]> predictedPricesBySymbol = Read.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> dataSource.prices) //
				.toMap();

		Map<String, float[]> returnsBySymbol = Read.from2(predictedPricesBySymbol) //
				.mapValue(ts::returns) //
				.toMap();

		Map<String, Float> excessReturnBySymbol = Read.from2(returnsBySymbol) //
				.mapValue(returns -> (float) (stat.meanVariance(returns).mean - dailyInterestRate)) //
				.toMap();

		String[] symbols = returnsBySymbol.keySet().toArray(new String[0]);
		int nSymbols = symbols.length;

		float[][] cov = To.arrayOfFloats(nSymbols, nSymbols, (i0, i1) -> {
			float[] returns0 = returnsBySymbol.get(symbols[i0]);
			float[] returns1 = returnsBySymbol.get(symbols[i1]);
			return (float) stat.covariance(returns0, returns1);
		});

		float[] returns = To.arrayOfFloats(symbols, excessReturnBySymbol::get);

		float[] allocations = cholesky.inverseMul(cov).apply(returns);

		return Read.range(nSymbols) //
				.map2(i -> symbols[i], i -> (double) allocations[i]) //
				.toList();
	}

}
