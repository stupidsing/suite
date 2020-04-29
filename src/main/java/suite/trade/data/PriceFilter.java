package suite.trade.data;

import static primal.statics.Fail.fail;

import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.Flt_Flt;
import suite.trade.Instrument;
import suite.trade.TimeRange;

public class PriceFilter {

	private TradeCfg cfg;
	private Flt_Flt priceFun;

	public PriceFilter(TradeCfg cfg, Flt_Flt priceFun) {
		this.cfg = cfg;
		this.priceFun = priceFun;
	}

	public DataSource dataSource(String symbol, TimeRange period) {
		var ds = cfg.dataSource(s(symbol), period);
		return DataSource.ofOhlcv(ds.ts,
				ReadFlt.from(ds.opens).mapFlt(priceFun).toArray(),
				ReadFlt.from(ds.closes).mapFlt(priceFun).toArray(),
				ReadFlt.from(ds.lows).mapFlt(priceFun).toArray(),
				ReadFlt.from(ds.highs).mapFlt(priceFun).toArray(),
				ds.volumes);
	}

	public Instrument queryCompany(String symbol) {
		return cfg.queryCompany(s(symbol));
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return cfg.quote(Read.from(symbols).map(this::s).toSet());
	}

	private String s(String symbol) {
		return symbol.startsWith("#") ? symbol.substring(1) : fail();
	}

}
