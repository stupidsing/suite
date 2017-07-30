package suite.trade.backalloc.strategy;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Trade_;
import suite.util.To;

public class KellyCriterion {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private double dailyInterestRate = Trade_.riskFreeInterestRate(1);

	public List<Pair<String, Double>> allocate(Streamlet2<String, float[]> predictedPricesBySymbol, double kellyReduction) {
		Map<String, float[]> returnsBySymbol = predictedPricesBySymbol //
				.mapValue(ts::returns) //
				.toMap();

		Map<String, Float> excessReturnBySymbol = Read //
				.from2(returnsBySymbol) //
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

		return Ints_ //
				.range(nSymbols) //
				.map2(i -> symbols[i], i -> (double) allocations[i] * kellyReduction) //
				.toList();
	}

}
