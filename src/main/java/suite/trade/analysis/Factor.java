package suite.trade.analysis;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.Vector_;
import suite.math.numeric.Statistic;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Usex;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.trade.data.HkexUtil;
import suite.util.Object_;
import ts.Quant;

public class Factor {

	private DataSource ids;

	private Configuration cfg;
	private Statistic stat = new Statistic();
	private Time now = Time.now();
	private Vector_ vec = new Vector_();

	public static Factor ofCrudeOil(Configuration cfg) {
		return of(cfg, Read.each(Usex.crudeOil));
	}

	public static Factor ofUsMarket(Configuration cfg) {
		return of(cfg, Read.each(Usex.dowJones, Usex.nasdaq, Usex.sp500));
	}

	public static Factor of(Configuration cfg, Streamlet<String> indices) {
		return new Factor(cfg, indices);
	}

	private Factor(Configuration cfg, Streamlet<String> indices) {
		this.cfg = cfg;

		AlignKeyDataSource<String> akds = cfg.dataSources(TimeRange.of(Time.MIN, now), indices);

		float[] indexPrices = akds.dsByKey //
				.map((symbol, ds) -> ds.prices) //
				.fold(new float[akds.ts.length], vec::add);

		ids = DataSource.of(akds.ts, indexPrices);
	}

	public List<Pair<Asset, Double>> query(Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(now), 250 * 3);

		return assets //
				.map2(asset -> project(ids, cfg.dataSource(asset.symbol), period)) //
				.sortByValue(Object_::compare) //
				.toList();
	}

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();

			DataSourceView<String, Double> dsv = DataSourceView.of(0, 64, akds,
					(symbol, ds, period) -> project(ids, dsBySymbol_.get(symbol), period));

			return index -> {
				float[] indexPrices = ids.prices;
				double indexReturn = Quant.return_(indexPrices[index - 2], indexPrices[index - 1]);

				return dsBySymbol //
						.map2((symbol, ds) -> indexReturn * dsv.get(symbol, index)) //
						.toList();
			};
		};
	}

	private double project(DataSource irds0, DataSource rds0, TimeRange period) {
		DataSource rds1 = rds0.range(period);
		DataSource irds1 = irds0.range(period).alignBeforePrices(rds1.ts);
		return stat.project(irds1.returns(), rds1.returns());
	}

}
