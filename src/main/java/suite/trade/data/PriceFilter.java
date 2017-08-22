package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.primitive.Flt_Flt;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;

public class PriceFilter {

	private Configuration cfg;
	private Flt_Flt priceFun;

	public PriceFilter(Configuration cfg, Flt_Flt priceFun) {
		this.cfg = cfg;
		this.priceFun = priceFun;
	}

	public DataSource dataSource(String symbol, TimeRange period) {
		DataSource ds = cfg.dataSource(s(symbol), period);
		return DataSource.ofOhlcv(ds.ts, //
				FltStreamlet.of(ds.opens).mapFlt(priceFun).toArray(), //
				FltStreamlet.of(ds.closes).mapFlt(priceFun).toArray(), //
				FltStreamlet.of(ds.lows).mapFlt(priceFun).toArray(), //
				FltStreamlet.of(ds.highs).mapFlt(priceFun).toArray(), //
				ds.volumes);
	}

	public Asset queryCompany(String symbol) {
		return cfg.queryCompany(s(symbol));
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return cfg.quote(Read.from(symbols).map(this::s).toSet());
	}

	private String s(String symbol) {
		if (symbol.startsWith("#"))
			return symbol.substring(1);
		else
			throw new RuntimeException();
	}

}
