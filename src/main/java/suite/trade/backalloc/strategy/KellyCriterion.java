package suite.trade.backalloc.strategy;

import static suite.util.Friends.forInt;

import java.util.List;

import suite.adt.pair.Pair;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.numeric.Statistic;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Trade_;
import suite.ts.TimeSeries;
import suite.util.To;

public class KellyCriterion {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private double dailyInterestRate = Trade_.riskFreeInterestRate(1);

	public List<Pair<String, Double>> allocate(Streamlet2<String, float[]> predictedPricesBySymbol, double kellyReduction) {
		var returnsBySymbol = predictedPricesBySymbol //
				.mapValue(ts::returns) //
				.toMap();

		var excessReturnBySymbol = Read //
				.from2(returnsBySymbol) //
				.mapValue(returns -> (float) (stat.meanVariance(returns).mean - dailyInterestRate)) //
				.toMap();

		var symbols = returnsBySymbol.keySet().toArray(new String[0]);
		var nSymbols = symbols.length;

		var cov = To.matrix(nSymbols, nSymbols, (i0, i1) -> {
			var returns0 = returnsBySymbol.get(symbols[i0]);
			var returns1 = returnsBySymbol.get(symbols[i1]);
			return (float) stat.covariance(returns0, returns1);
		});

		var returns = To.vector(symbols, excessReturnBySymbol::get);
		var allocations = cholesky.inverseMul(cov).apply(returns);

		return forInt(nSymbols) //
				.map2(i -> symbols[i], i -> (double) allocations[i] * kellyReduction) //
				.toList();
	}

}
