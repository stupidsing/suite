package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.math.Cholesky;
import suite.math.TimeSeries;
import suite.streamlet.Read;
import suite.trade.data.DataSource;
import suite.util.To;

public class KellyAssetAllocator implements AssetAllocator {

	private Statistic stat = new Statistic();
	private TimeSeries timeSeries = new TimeSeries();

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceByStockCode, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		// TODO this should be the expected returns, not past returns!
		Map<String, DataSource> predictedReturnsByStockCode = dataSourceByStockCode;

		Map<String, float[]> returnsByStockCode = Read.from2(predictedReturnsByStockCode) //
				.mapValue(dataSource -> timeSeries.returns(dataSource.prices)) //
				.toMap();

		Map<String, Double> excessReturnByStockCode = Read.from2(predictedReturnsByStockCode) //
				.mapValue(dataSource -> {
					double price0 = dataSource.first().price;
					double pricex = dataSource.last().price;
					double nYears = dataSource.nYears();
					int nDays = dataSource.dates.length;
					double dailyInterestRate = Math.exp(Math.log1p(stat.riskFreeInterestRate) * nYears / nDays);
					return pricex / price0 - dailyInterestRate;
				}) //
				.toMap();

		String[] stockCodes = returnsByStockCode.keySet().toArray(new String[0]);
		int nStockCodes = stockCodes.length;

		float[][] cov = To.arrayOfFloats(nStockCodes, nStockCodes, (i0, i1) -> {
			float[] returns0 = returnsByStockCode.get(stockCodes[i0]);
			float[] returns1 = returnsByStockCode.get(stockCodes[i1]);
			return (float) stat.covariance(returns0, returns1);
		});

		float[] returns = To.arrayOfFloats(nStockCodes, i -> excessReturnByStockCode.get(stockCodes[i]).floatValue());

		float[] allocations = new Cholesky().inverseMul(cov).apply(returns);

		return Read.range(nStockCodes) //
				.map2(i -> stockCodes[i], i -> (double) allocations[i]) //
				.filterValue(potential -> 0d < potential) //
				.toList();
	}

}
