package suite.trade.data;

import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.fp.Funs.Fun;
import primal.fp.Funs2.Fun2;
import primal.os.Log_;
import primal.streamlet.Streamlet;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.Usex;
import suite.trade.data.Broker.Hsbc;

public class TradeCfgImpl implements TradeCfg {

	private Broker broker = new Hsbc();
	private Hkd hkd = new Hkd();
	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private PriceFilter pf = new PriceFilter(this, price -> price / 1000f);
	private Quandl quandl = new Quandl();
	private Sina sina = new Sina();
	private Yahoo yahoo = new Yahoo();

	private Src srcForex = new Src(null, yahoo::quote, yahoo::dataSourceL1);
	private Src srcHkd__ = new Src(hkd::queryCompany, hkd::quote, hkd::dataSource);
	private Src srcHkex_ = new Src(hkex::queryCompany, sina::quote, yahoo::dataSourceL1);
	private Src srcIndex = new Src(hkd::queryCompany, yahoo::quote, yahoo::dataSourceL1);
	private Src srcNymex = new Src(null, yahoo::quote, quandl::dataSourceCsv);
	private Src srcNone_ = new Src(null, yahoo::quote, null);
	private Src srcPf___ = new Src(pf::queryCompany, pf::quote, pf::dataSource);

	private class Src {
		private Fun<String, Instrument> queryFun;
		private Fun<Set<String>, Map<String, Float>> quoteFun;
		private Fun2<String, TimeRange, DataSource> dataSourceFun;

		private Src( //
				Fun<String, Instrument> queryFun, //
				Fun<Set<String>, Map<String, Float>> quoteFun, //
				Fun2<String, TimeRange, DataSource> dataSourceFun) {
			this.queryFun = queryFun;
			this.quoteFun = quoteFun;
			this.dataSourceFun = dataSourceFun;
		}
	}

	public DataSource dataSource(String symbol) {
		return dataSource_(symbol, TimeRange.ages());
	}

	public DataSource dataSource(String symbol, TimeRange period) {
		return dataSource_(symbol, period);
	}

	public Streamlet<Instrument> queryCompanies() {
		return hkex.queryCompanies().filter(this::filter);
	}

	public Instrument queryCompany(String symbol) {
		return filter(symbol) ? src(symbol).queryFun.apply(symbol) : null;
	}

	public Streamlet<Instrument> queryCompaniesByMarketCap(Time time) {
		var year = time.year() - 1;

		return Read //
				.from(hkexFactBook.queryCompaniesByMarketCap(year)) //
				.map(this::queryCompany) //
				.filter(this::filter);
	}

	public Streamlet<Trade> queryHistory() {
		return broker.queryHistory();
	}

	public Map<String, Float> quote(Set<String> symbols) {
		return quote_(symbols);
	}

	public double transactionFee(double transactionAmount) {
		return broker.transactionFee(transactionAmount);
	}

	private Map<String, Float> quote_(Set<String> symbols) {
		var map = new HashMap<Fun<Set<String>, Map<String, Float>>, Set<String>>();

		for (var symbol : symbols)
			if (filter(symbol))
				map.computeIfAbsent(src(symbol).quoteFun, s -> new HashSet<>()).add(symbol);

		return Read //
				.from2(map) //
				.concatMap2((quoteFun, symbols_) -> Read.from2(quoteFun.apply(symbols_))) //
				.toMap();
	}

	private DataSource dataSource_(String symbol, TimeRange period) {
		var ds = src(symbol).dataSourceFun.apply(symbol, period);
		var epx = ds.last().t0;
		var now = min(Time.now().epochSec(), period.to.epochSec());
		if (epx + 7 * 86400 * 1000l < now)
			Log_.warn("ancient data: " + symbol + " " + Time.ofEpochSec(epx));
		return ds;
	}

	private Src src(String symbol) {
		if (symbol.endsWith("=X"))
			return srcForex;
		else if (Equals.string(symbol, Instrument.cashSymbol))
			return srcHkd__;
		else if (symbol.endsWith(".HK"))
			return srcHkex_;
		else if (symbol.startsWith("^") || Equals.string(symbol, Usex.nasdaq))
			return srcIndex;
		else if (Equals.string(symbol, "CL=F") || symbol.endsWith(".NYM"))
			return srcNymex;
		else if (Boolean.FALSE)
			return srcNone_;
		else if (symbol.startsWith("#"))
			return srcPf___;
		else
			return fail(symbol);
	}

	private boolean filter(Instrument instrument) {
		return filter(instrument.symbol);
	}

	private boolean filter(String symbol) {
		return !Trade_.blackList.contains(symbol);
	}

}
