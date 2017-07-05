package suite.trade.analysis;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Cleanse;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class Factor {

	private DataSource ids;

	private Configuration cfg;
	private Cleanse cleanse = new Cleanse();
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private Time now = Time.now();
	private TimeSeries ts = new TimeSeries();

	public static Factor ofCrudeOil(Configuration cfg) {
		return new Factor(cfg, Read.each("CLQ17.NYM")); // "CL=F"
	}

	public static Factor ofUsMarket(Configuration cfg) {
		return new Factor(cfg, Read.each("^DJI", "^GSPC", "NDAQ"));
	}

	private Factor(Configuration cfg, Streamlet<String> indices) {
		this.cfg = cfg;

		AlignKeyDataSource<String> akds = cfg.dataSources(indices, TimeRange.of(Time.MIN, now));

		float[] indexPrices = akds.dsByKey //
				.map((symbol, ds) -> cleanse.removeZeroes(ds.prices)) //
				.fold(new float[akds.ts.length], mtx::add);

		ids = new DataSource(akds.ts, indexPrices);
	}

	public List<Pair<Asset, Double>> query(Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);

		return assets //
				.map2(asset -> correlate(ids, cfg.dataSource(asset.symbol), period)) //
				.sortByValue(Object_::compare) //
				.toList();
	}

	public BackAllocator backAllocator() {
		return (dsBySymbol, ts_) -> {
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();

			DataSourceView<String, Double> dsv = DataSourceView.of(0, 64, dsBySymbol, ts_,
					(symbol, ds, period) -> correlate(ids, dsBySymbol_.get(symbol), period));

			return (time, index) -> {
				float[] indexPrices = ids.prices;
				double indexReturn = Quant.return_(indexPrices[index - 2], indexPrices[index - 1]);

				return dsBySymbol //
						.map2((symbol, ds) -> indexReturn * dsv.get(symbol, time)) //
						.toList();
			};
		};
	}

	private double correlate(DataSource irds0, DataSource rds0, TimeRange period) {
		DataSource rds1 = rds0.range(period);
		DataSource irds1 = irds0.range(period).alignBeforePrices(rds1.ts);
		return stat.correlation(ts.returns(irds1.prices), ts.returns(rds1.prices));
	}

}
