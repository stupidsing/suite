package suite.trade.backalloc;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.analysis.Factor;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class BackAllocConfigurations {

	private Configuration cfg;
	private Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;
	private Sink<String> log;

	public final Bacs bacs;

	public class Bacs {
		private BackAllocator ba_bb = BackAllocator_.bollingerBands().filterByIndex(cfg).holdMinimum(9);
		private BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();
		private BackAllocator ba_mix = BackAllocator_.sum(ba_bb, ba_facoil);

		public final BackAllocConfiguration bac_bb = ba_bb.cfgUnl(fun);
		public final BackAllocConfiguration bac_donchian = BackAllocator_.donchian(9).cfgUnl(fun);
		public final BackAllocConfiguration bac_ema = BackAllocator_.ema().pick(3).cfgUnl(fun);
		public final BackAllocConfiguration bac_facoil = ba_facoil.cfgUnl(fun);
		public final BackAllocConfiguration bac_hsi = BackAllocConfiguration.ofSingle(Asset.hsi);
		public final BackAllocConfiguration bac_mix = ba_mix.cfgUnl(fun);
		public final BackAllocConfiguration bac_pmamr = MovingAvgMeanReversionBackAllocator0.of(log).cfgUnl(fun);
		public final BackAllocConfiguration bac_pmmmr = BackAllocator_.movingMedianMeanRevn().holdMinimum(9).cfgUnl(fun);
		public final BackAllocConfiguration bac_revco = ReverseCorrelateBackAllocator.of().cfgUnl(fun);
		public final BackAllocConfiguration bac_rsi = BackAllocator_.rsi().cfgUnl(fun);
		public final BackAllocConfiguration bac_sell = BackAllocator_.cash().cfgUnl(fun);
		public final BackAllocConfiguration bac_tma = BackAllocator_.tripleMovingAvgs().cfgUnl(fun);

		public BackAllocConfiguration questoaQuella(String symbol0, String symbol1) {
			Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
			BackAllocator backAllocator = BackAllocator_.questoQuella(symbol0, symbol1);
			return new BackAllocConfiguration(time -> assets, backAllocator);
		}
	}

	public BackAllocConfigurations(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		fun = cfg::queryCompaniesByMarketCap;
		this.log = log;
		bacs = new Bacs();
	}

}
