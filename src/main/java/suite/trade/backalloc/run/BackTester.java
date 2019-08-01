package suite.trade.backalloc.run;

import static java.lang.Math.expm1;
import static java.lang.Math.log1p;

import primal.Verbs.Compare;
import primal.fp.FunUtil;
import primal.fp.Funs.Sink;
import suite.math.numeric.Statistic;
import suite.primitive.AsDbl;
import suite.primitive.Floats_;
import suite.primitive.AsFlt;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Instrument;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.util.To;

public class BackTester {

	private float initial = 1000000f;
	private Sink<String> log = FunUtil.nullSink();
	private TradeCfg cfg = new TradeCfgImpl();
	private Statistic stat = new Statistic();

	public Simulate backTest(BackAllocator backAllocator, TimeRange period) {
		var instruments0 = cfg.queryCompaniesByMarketCap(period.fr); // hkex.getCompanies()
		return backTest(backAllocator, period, instruments0);
	}

	public Simulate backTest(BackAllocator backAllocator, TimeRange period, Streamlet<Instrument> instruments) {
		return BackAllocTester.of(cfg, period, instruments, backAllocator, log).simulate(initial);
	}

	public <T> String conclude(Streamlet2<T, Simulate> simulationsByKey) {
		var results0 = simulationsByKey //
				.map((key, simulate) -> "TEST = " + key + ", " + simulate.conclusion());

		var results1 = simulationsByKey //
				.filterValue(sim -> sim.exception == null) //
				.groupBy(sims -> {
					var txFee = sims.toDouble(AsDbl.sum(sim -> cfg.transactionFee(sim.account.transactionAmount())));

					var returns = sims //
							.collect(AsFlt.lift(sim -> (float) sim.annualReturn)) //
							.toArray();

					var mv = stat.meanVariance(returns);
					var logCagr = Floats_.of(returns).mapFlt(return_ -> (float) log1p(return_)).average();

					return ">> cagr = " + To.string(expm1(logCagr)) //
							+ ", sharpe = " + To.string(mv.mean / mv.standardDeviation()) //
							+ ", txFee = " + To.string(txFee / sims.size());
				}) //
				.map((key, summary) -> "TEST = " + key + " " + summary);

		return Streamlet.concat(results0, results1).sort(Compare::objects).toString();
	}

}
