package suite.trade.analysis;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.Verbs.Compare;
import primal.adt.Pair;
import primal.streamlet.Streamlet;
import suite.math.linalg.Vector;
import suite.math.numeric.Statistic;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Usex;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.trade.data.TradeCfg;
import suite.ts.Quant;

public class Factor {

	private DataSource ids;

	private TradeCfg cfg;
	private Statistic stat = new Statistic();
	private Time now = Time.now();
	private Vector vec = new Vector();

	public static Factor ofCrudeOil(TradeCfg cfg) {
		return new Factor(cfg, Read.each(Usex.crudeOil));
	}

	public Factor(TradeCfg cfg, Streamlet<String> indexSymbols_) {
		this.cfg = cfg;

		var akds = cfg.dataSources(TimeRange.of(Time.MIN, now), indexSymbols_);

		var indexPrices = akds.dsByKey //
				.map((symbol, ds) -> ds.prices) //
				.fold(new float[akds.ts.length], vec::add);

		ids = DataSource.of(akds.ts, indexPrices);
	}

	public List<Pair<Instrument, Double>> query(Streamlet<Instrument> instruments) {
		var period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(now), 250 * 3);

		return instruments //
				.map2(instrument -> project(ids, cfg.dataSource(instrument.symbol), period)) //
				.sortByValue(Compare::objects) //
				.toList();
	}

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			var dsBySymbol = akds.dsByKey;
			var dsBySymbol_ = dsBySymbol.toMap();

			var dsv = DataSourceView.of(0, 64, akds,
					(symbol, ds, period) -> project(ids, dsBySymbol_.get(symbol), period));

			return index -> {
				var indexPrices = ids.prices;
				var indexReturn = Quant.return_(indexPrices[index - 2], indexPrices[index - 1]);

				return dsBySymbol //
						.map2((symbol, ds) -> indexReturn * dsv.get(symbol, index)) //
						.toList();
			};
		};
	}

	private double project(DataSource irds0, DataSource rds0, TimeRange period) {
		var rds1 = rds0.range(period);
		var irds1 = irds0.range(period).alignBeforePrices(rds1.ts);
		return stat.project(irds1.returns(), rds1.returns());
	}

}
