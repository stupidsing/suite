package suite.trade.backalloc.strategy;

import java.util.List;
import java.util.Map;
import java.util.Random;

import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.util.Object_;
import suite.util.To;

/**
 * Genetic programming on asset allocation, and use Monte-carlo simulation to
 * evaluate outcomes.
 *
 * @author ywsing
 */
public class MonteCarloBackAllocator implements BackAllocator {

	private Random random = new Random(Time.of(2017, 1, 1).epochSec());

	@Override
	public OnDateTime allocate(Streamlet2<String, DataSource> dsBySymbol, int[] indices) {
		return (time, index) -> {
			Map<String, float[]> returnsBySymbol = dsBySymbol.mapValue(DataSource::returns).toMap();
			String[] symbols = returnsBySymbol.keySet().toArray(new String[0]);

			List<float[]> portfolios = IntStreamlet //
					.range(99) //
					.map(i -> randomPortfolio(symbols)) //
					.toList();

			for (int i = 0; i < 128; i++) {
				List<float[]> portfolios1 = Read.from(portfolios) //
						.map2(portfolio -> evaluate(symbols, portfolio, returnsBySymbol, index)) //
						.sortByValue((o0, o1) -> Object_.compare(o1, o0)) //
						.take(128) //
						.keys() //
						.toList();

				int size = portfolios.size();

				for (int j = 0; j < 12; j++) {
					float[] portfolio = portfolios.get(random.nextInt(size));
					portfolios.add(mutate(symbols, portfolio));
				}

				for (int j = 0; j < 12; j++) {
					float[] pa = portfolios.get(random.nextInt(size));
					float[] pb = portfolios.get(random.nextInt(size));
					portfolios1.add(crossover(pa, pb));
				}

				portfolios = portfolios1;
			}

			float[] portfolio = portfolios.get(0);

			return IntStreamlet //
					.range(symbols.length) //
					.map2(i -> symbols[i], i -> (double) portfolio[i]) //
					.toList();
		};
	}

	private double evaluate(String[] symbols, float[] p, Map<String, float[]> returnsBySymbol, int index) {
		int d = random.nextInt(index);
		double sum = 0d;
		for (int i = 0; i < symbols.length; i++) {
			String symbol = symbols[i];
			sum += p[i] * returnsBySymbol.get(symbol)[d];
		}
		return sum;
	}

	private float[] randomPortfolio(String[] symbols) {
		return fair(To.arrayOfFloats(symbols.length, i -> random.nextFloat()));
	}

	private float[] mutate(String[] symbols, float[] portfolio) {
		int size = symbols.length;
		for (int i = 0; i < 3; i++)
			portfolio[random.nextInt(size)] = 1f;
		fair(portfolio);
		return portfolio;
	}

	private float[] crossover(float[] p0, float[] p1) {
		return To.arrayOfFloats(p0.length, i -> (p0[i] + p1[i]) * .5f);
	}

	private float[] fair(float[] p) {
		double sum = 0d;
		for (int i = 0; i < p.length; i++)
			sum += p[i];
		double invSum = 1d / sum;
		for (int i = 0; i < p.length; i++)
			p[i] *= invSum;
		return p;
	}

}
