package suite.trade.analysis;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.streamlet.As;
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

	private DataSource irds;

	private Configuration cfg;
	private Cleanse cleanse = new Cleanse();
	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private Time today = Time.now().date();
	private TimeSeries ts = new TimeSeries();

	public static Factor ofCrudeOil(Configuration cfg) {
		return new Factor(cfg, Read.each("CLQ17.NYM")); // "CL=F"
	}

	public static Factor ofUsMarket(Configuration cfg) {
		return new Factor(cfg, Read.each("^DJI", "^GSPC", "NDAQ"));
	}

	private Factor(Configuration cfg, Streamlet<String> indices) {
		this.cfg = cfg;

		AlignKeyDataSource<String> akds = indices //
				.map2(symbol -> cleanse.removeZeroes(cfg.dataSource(symbol, TimeRange.of(Time.MIN, today)))) //
				.collect(As::streamlet2) //
				.apply(DataSource::alignAll);

		float[] indexReturns = akds.dsByKey //
				.map((symbol, ds) -> ts.returns(ds.prices)) //
				.fold(new float[akds.ts.length], mtx::add);

		irds = new DataSource(akds.ts, indexReturns);
	}

	public List<Pair<Asset, Double>> query(Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(HkexUtil.getOpenTimeBefore(Time.now()), 250 * 3);

		return assets //
				.map2(asset -> correlate(irds, returns(cfg.dataSource(asset.symbol)), period)) //
				.sortByValue(Object_::compare) //
				.toList();
	}

	public BackAllocator backAllocator() {
		return (dsBySymbol, ts_) -> {
			Map<String, DataSource> returnDsBySymbol = dsBySymbol.mapValue(this::returns).toMap();

			DataSourceView<String, Double> dsv = DataSourceView.of(0, 64, dsBySymbol, ts_,
					(symbol, ds, period) -> correlate(irds, returnDsBySymbol.get(symbol), period));

			return (time, index) -> {
				float indexReturn = irds.last(time).t1;

				return dsBySymbol //
						.map2((symbol, ds) -> indexReturn * dsv.get(symbol, time)) //
						.toList();
			};
		};
	}

	private DataSource returns(DataSource ds) {
		return new DataSource(ds.ts, ts.returns(ds.prices));
	}

	private double correlate(DataSource irds0, DataSource rds0, TimeRange period) {
		DataSource rds1 = rds0.range(period);
		DataSource irds1 = irds0.range(period).alignBeforePrices(rds1.ts);
		return stat.correlation(irds1.prices, rds1.prices);
	}

}
