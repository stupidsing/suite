package suite.trade.analysis;

import static suite.util.Streamlet_.forInt;

import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet;
import suite.math.linalg.Matrix;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.streamlet.As;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.trade.data.TradeCfg;
import suite.ts.Quant;

public class FactorLr {

	private long[] timestamps;
	private Streamlet<String> indexSymbols;
	private List<float[]> indexPrices;

	private TradeCfg cfg;
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private Time now = Time.now();

	public FactorLr(TradeCfg cfg, Streamlet<String> indexSymbols_) {
		this.cfg = cfg;

		indexSymbols = indexSymbols_.collect();
		var akds = cfg.dataSources(TimeRange.ages(), indexSymbols_);
		var dsBySymbol = akds.dsByKey.toMap();

		timestamps = akds.ts;
		indexPrices = indexSymbols.map(symbol -> dsBySymbol.get(symbol).prices).toList();
	}

	public Map<Instrument, String> query(Streamlet<Instrument> instruments) {
		var period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(now), 250 * 3);

		return instruments //
				.map2(instrument -> ols(cfg.dataSource(instrument.symbol), period).toString()) //
				.toMap();
	}

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			var dsBySymbol = akds.dsByKey;
			var dsBySymbol_ = dsBySymbol.toMap();

			var dsv = DataSourceView.of(0, 64, akds, (symbol, ds, period) -> ols(dsBySymbol_.get(symbol), period));

			return index -> {
				var xs = forInt(indexSymbols.size()) //
						.collect(As.floats(i -> {
							var indexPrices_ = indexPrices.get(i);
							return (float) Quant.return_(indexPrices_[index - 2], indexPrices_[index - 1]);
						})) //
						.toArray();

				return dsBySymbol //
						.map2((symbol, ds) -> (double) dsv.get(symbol, index).predict(xs)) //
						.toList();
			};
		};
	}

	private LinearRegression ols(DataSource rds0, TimeRange period) {
		var ys = rds0.range(period);

		var returns_ = Read //
				.from(indexPrices) //
				.map(prices -> DataSource.of(timestamps, prices).range(period).alignBeforePrices(ys.ts).returns()) //
				.toArray(float[].class);

		var xs = mtx.transpose(returns_);
		return stat.linearRegression(xs, ys.returns(), indexSymbols.toArray(String.class));
	}

}
