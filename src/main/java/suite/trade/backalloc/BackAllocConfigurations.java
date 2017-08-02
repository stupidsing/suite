package suite.trade.backalloc;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.analysis.Factor;
import suite.trade.backalloc.strategy.BackAllocatorOld_;
import suite.trade.backalloc.strategy.BackAllocator_;
import suite.trade.backalloc.strategy.MovingAvgMeanReversionBackAllocator0;
import suite.trade.backalloc.strategy.ReverseCorrelateBackAllocator;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class BackAllocConfigurations {

	private Configuration cfg;
	private Sink<String> log;

	public final Bacs bacs;

	public class Bacs {
		private Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;
		private Fun<Time, Streamlet<Asset>> fun_hsi = time -> Read.each(Asset.hsi);

		private BackAllocator ba_bb = BackAllocator_.bollingerBands().filterByIndex(cfg).holdExtend(8);
		private BackAllocator ba_donchian = BackAllocator_.donchian(9).holdExtend(2).pick(5);
		private BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();

		public final BackAllocConfiguration bac_bb = ba_bb.cfgUnl(fun);
		public final BackAllocConfiguration bac_donchian = ba_donchian.cfgUnl(fun);
		public final BackAllocConfiguration bac_ema = BackAllocator_.ema().pick(3).cfgUnl(fun);
		public final BackAllocConfiguration bac_pmamr = MovingAvgMeanReversionBackAllocator0.of(log).cfgUnl(fun);
		public final BackAllocConfiguration bac_pmmmr = BackAllocatorOld_.movingMedianMeanRevn().holdExtend(9).cfgUnl(fun);
		public final BackAllocConfiguration bac_revco = ReverseCorrelateBackAllocator.of().cfgUnl(fun);
		public final BackAllocConfiguration bac_sell = BackAllocator_.cash().cfgUnl(fun);
		public final BackAllocConfiguration bac_tma = BackAllocator_.tripleMovingAvgs().cfgUnl(fun);

		public final Streamlet2<String, BackAllocConfiguration> bacByName = Read //
				.<String, BackAllocConfiguration>empty2() //
				.cons("hsi", BackAllocConfiguration.ofSingle(Asset.hsi)) //
				.cons("bb", bac_bb) //
				.cons("bbSlope", BackAllocatorOld_.bbSlope().cfgUnl(fun)) //
				.cons("donchian", bac_donchian) //
				.cons("ema", bac_ema) //
				.cons("facoil", ba_facoil.cfgUnl(fun)) //
				.cons("january", BackAllocator_.ofSingle(Asset.hsiSymbol).january().cfgUnl(fun_hsi)) //
				.cons("lr", BackAllocator_.lastReturn(0, 2).cfgUnl(fun)) //
				.cons("mix", BackAllocator_.sum(ba_bb, ba_donchian).cfgUnl(fun)) //
				.cons("pmamr", bac_pmamr) //
				.cons("pmmmr", bac_pmmmr) //
				.cons("revco", bac_revco) //
				.cons("revdd", BackAllocatorOld_.revDrawdown().holdExtend(40).cfgUnl(fun)) //
				.cons("rsi", BackAllocator_.rsi().cfgUnl(fun)) //
				.cons("sar", BackAllocator_.sar().cfgUnl(fun)) //
				.cons("sellInMay", BackAllocator_.ofSingle(Asset.hsiSymbol).sellInMay().cfgUnl(fun_hsi)) //
				.cons("turtles", BackAllocator_.turtles(20, 10, 55, 20).cfgUnl(fun)) //
				.cons("tma", bac_tma) //
				.cons("tma1", BackAllocator_.tripleExpGeometricMovingAvgs().cfgUnl(fun)) //
				.cons("xma", BackAllocator_.xma().cfgUnl(fun));

		public BackAllocConfiguration questoaQuella(String symbol0, String symbol1) {
			Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
			BackAllocator backAllocator = BackAllocatorOld_.questoQuella(symbol0, symbol1);
			return new BackAllocConfiguration(time -> assets, backAllocator);
		}
	}

	public BackAllocConfigurations(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		this.log = log;
		bacs = new Bacs();
	}

}
