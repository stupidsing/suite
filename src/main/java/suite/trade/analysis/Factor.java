package suite.trade.analysis;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.util.Object_;

public class Factor {

	private DataSource ids;

	private Configuration cfg;
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private Time now = Time.now();

	public static Factor ofCrudeOil(Configuration cfg) {
		return of(cfg, Read.each("CLQ17.NYM")); // "CL=F"
	}

	public static Factor ofUsMarket(Configuration cfg) {
		return of(cfg, Read.each("^DJI", "^GSPC", "NDAQ"));
	}

	public static Factor of(Configuration cfg, Streamlet<String> indices) {
		return new Factor(cfg, indices);
	}

	private Factor(Configuration cfg, Streamlet<String> indices) {
		this.cfg = cfg;

		AlignKeyDataSource<String> akds = cfg.dataSources(TimeRange.of(Time.MIN, now), indices);

		float[] indexPrices = akds.dsByKey //
				.map((symbol, ds) -> ds.prices) //
				.fold(new float[akds.ts.length], mtx::add);

		ids = DataSource.of(akds.ts, indexPrices);
	}

	public List<Pair<Asset, Double>> query(Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(now), 250 * 3);

		return assets //
				.map2(asset -> correlate(ids, cfg.dataSource(asset.symbol), period)) //
				.sortByValue(Object_::compare) //
				.toList();
	}

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();

			DataSourceView<String, Double> dsv = DataSourceView.of(0, 64, akds, indices,
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
		return stat.correlation(irds1.returns(), rds1.returns());
	}

}
