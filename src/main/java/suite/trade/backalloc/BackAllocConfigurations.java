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
	public final BackAllocConfiguration bac_bb;
	public final BackAllocConfiguration bac_donchian;
	public final BackAllocConfiguration bac_ema;
	public final BackAllocConfiguration bac_hsi;
	public final BackAllocConfiguration bac_mix;
	public final BackAllocConfiguration bac_pmamr;
	public final BackAllocConfiguration bac_facoil;
	public final BackAllocConfiguration bac_pmmmr;
	public final BackAllocConfiguration bac_revco;
	public final BackAllocConfiguration bac_rsi;
	public final BackAllocConfiguration bac_sell;
	public final BackAllocConfiguration bac_tma;

	public BackAllocConfigurations(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		Fun<Time, Streamlet<Asset>> fun = cfg::queryCompaniesByMarketCap;

		BackAllocator ba_bb = BackAllocator_.bollingerBands().filterByIndex(cfg).holdMinimum(9);
		BackAllocator ba_facoil = Factor.ofCrudeOil(cfg).backAllocator().longOnly().pick(3).even();

		bac_bb = ba_bb.cfgUnl(fun);
		bac_donchian = BackAllocator_.donchian(9).cfgUnl(fun);
		bac_ema = BackAllocator_.ema().pick(3).cfgUnl(fun);
		bac_facoil = ba_facoil.cfgUnl(fun);
		bac_hsi = BackAllocConfiguration.ofSingle(Asset.hsi);
		bac_mix = BackAllocator_.sum(ba_bb, ba_facoil).cfgUnl(fun);
		bac_pmamr = MovingAvgMeanReversionBackAllocator0.of(log).cfgUnl(fun);
		bac_pmmmr = BackAllocator_.movingMedianMeanRevn().holdMinimum(9).cfgUnl(fun);
		bac_revco = ReverseCorrelateBackAllocator.of().cfgUnl(fun);
		bac_rsi = BackAllocator_.rsi().cfgUnl(fun);
		bac_sell = BackAllocator_.cash().cfgUnl(fun);
		bac_tma = BackAllocator_.tripleMovingAvgs().cfgUnl(fun);
	}

	public BackAllocConfiguration questoaQuella(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		BackAllocator backAllocator = BackAllocator_.questoQuella(symbol0, symbol1);
		return new BackAllocConfiguration(time -> assets, backAllocator);
	}

}
