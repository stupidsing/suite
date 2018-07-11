package suite.trade.data; import static suite.util.Friends.fail;

import java.util.Map;
import java.util.Set;

import suite.primitive.Floats_;
import suite.primitive.Flt_Flt;
import suite.streamlet.Read;
import suite.trade.Asset;
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
		return DataSource.ofOhlcv(ds.ts, //
				Floats_.of(ds.opens).mapFlt(priceFun).toArray(), //
				Floats_.of(ds.closes).mapFlt(priceFun).toArray(), //
				Floats_.of(ds.lows).mapFlt(priceFun).toArray(), //
				Floats_.of(ds.highs).mapFlt(priceFun).toArray(), //
				ds.volumes);
	}

	public Asset queryCompany(String symbol) {
		return cfg.queryCompany(s(symbol));
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return cfg.quote(Read.from(symbols).map(this::s).toSet());
	}

	private String s(String symbol) {
		return symbol.startsWith("#") ? symbol.substring(1) : fail();
	}

}
