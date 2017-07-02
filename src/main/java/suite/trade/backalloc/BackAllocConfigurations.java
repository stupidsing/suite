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

		bac_bb = BackAllocator_.bollingerBands().filterByIndex(cfg).holdMinimum(9).bacUnl(fun);
		bac_donchian = BackAllocator_.donchian(9).bacUnl(fun);
		bac_ema = BackAllocator_.ema().top(3).bacUnl(fun);
		bac_facoil = Factor.ofCrudeOil(cfg).backAllocator().bacUnl(fun);
		bac_hsi = BackAllocConfiguration.ofSingle(Asset.hsi);
		bac_pmamr = MovingAvgMeanReversionBackAllocator0.of(log).bacUnl(fun);
		bac_pmmmr = BackAllocator_.movingMedianMeanRevn().holdMinimum(9).bacUnl(fun);
		bac_revco = ReverseCorrelateBackAllocator.of().bacUnl(fun);
		bac_rsi = BackAllocator_.rsi().bacUnl(fun);
		bac_sell = BackAllocator_.cash().bacUnl(fun);
		bac_tma = BackAllocator_.tripleMovingAvgs().bacUnl(fun);
	}

	public BackAllocConfiguration questoaQuella(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		BackAllocator backAllocator = BackAllocator_.questoQuella(symbol0, symbol1);
		return new BackAllocConfiguration(time -> assets, backAllocator);
	}

}
