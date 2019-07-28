package suite.trade.backalloc.strategy;

import static suite.util.Streamlet_.forInt;

import java.util.Map;
import java.util.Random;

import suite.object.Object_;
import suite.streamlet.Read;
import suite.trade.Time;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
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
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		var dsBySymbol = akds.dsByKey;

		return index -> {
			var returnsBySymbol = dsBySymbol.mapValue(DataSource::returns).toMap();
			var symbols = returnsBySymbol.keySet().toArray(new String[0]);

			var portfolios = forInt(99) //
					.map(i -> randomPortfolio(symbols)) //
					.toList();

			for (var i = 0; i < 128; i++) {
				var portfolios1 = Read //
						.from(portfolios) //
						.map2(portfolio -> evaluate(symbols, portfolio, returnsBySymbol, index)) //
						.sortByValue((o0, o1) -> Object_.compare(o1, o0)) //
						.take(128) //
						.keys() //
						.toList();

				var size = portfolios.size();

				for (var j = 0; j < 12; j++) {
					var portfolio = portfolios.get(random.nextInt(size));
					portfolios.add(mutate(symbols, portfolio));
				}

				for (var j = 0; j < 12; j++) {
					var pa = portfolios.get(random.nextInt(size));
					var pb = portfolios.get(random.nextInt(size));
					portfolios1.add(crossover(pa, pb));
				}

				portfolios = portfolios1;
			}

			var portfolio = portfolios.get(0);

			return forInt(symbols.length) //
					.map2(i -> symbols[i], i -> (double) portfolio[i]) //
					.toList();
		};
	}

	private double evaluate(String[] symbols, float[] p, Map<String, float[]> returnsBySymbol, int index) {
		var d = random.nextInt(index);
		var sum = 0d;
		for (var i = 0; i < symbols.length; i++) {
			var symbol = symbols[i];
			sum += p[i] * returnsBySymbol.get(symbol)[d];
		}
		return sum;
	}

	private float[] randomPortfolio(String[] symbols) {
		return fair(To.vector(symbols.length, i -> random.nextFloat()));
	}

	private float[] mutate(String[] symbols, float[] portfolio) {
		var size = symbols.length;
		for (var i = 0; i < 3; i++)
			portfolio[random.nextInt(size)] = 1f;
		fair(portfolio);
		return portfolio;
	}

	private float[] crossover(float[] p0, float[] p1) {
		return To.vector(p0.length, i -> (p0[i] + p1[i]) * .5f);
	}

	private float[] fair(float[] p) {
		var sum = 0d;
		for (var i = 0; i < p.length; i++)
			sum += p[i];
		var invSum = 1d / sum;
		for (var i = 0; i < p.length; i++)
			p[i] *= invSum;
		return p;
	}

}
