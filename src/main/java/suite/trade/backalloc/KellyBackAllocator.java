package suite.trade.backalloc;

import java.util.List;
import java.util.Map;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
import suite.streamlet.IntStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class KellyBackAllocator implements BackAllocator {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private Fun<Streamlet2<String, float[]>, Streamlet2<String, float[]>> predict = st -> st;

	public static BackAllocator of(Fun<Streamlet2<String, float[]>, Streamlet2<String, float[]>> predict) {
		return new KellyBackAllocator(predict);
	}

	private KellyBackAllocator(Fun<Streamlet2<String, float[]>, Streamlet2<String, float[]>> predict) {
		this.predict = predict;
	}

	@Override
	public OnDateTime allocate(Streamlet2<String, DataSource> dataSourceBySymbol, List<Time> times) {
		double dailyInterestRate = Trade_.riskFreeInterestRate(1);

		// TODO this should be the expected returns, not past returns!
		Streamlet2<String, float[]> pricesBySymbol = dataSourceBySymbol //
				.mapValue(dataSource -> dataSource.prices) //
				.collect(As::streamlet2);

		Streamlet2<String, float[]> predictedPricesBySymbol = predict.apply(pricesBySymbol);

		return (time, index) -> {
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

			return IntStreamlet //
					.range(nSymbols) //
					.map2(i -> symbols[i], i -> (double) allocations[i]) //
					.toList();
		};
	}

}
