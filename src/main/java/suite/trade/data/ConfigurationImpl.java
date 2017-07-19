package suite.trade.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Broker.Hsbc;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil2.Fun2;
import suite.util.String_;

public class ConfigurationImpl implements Configuration {

	private Broker broker = new Hsbc();
	private Google google = new Google();
	private Hkd hkd = new Hkd();
	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private Quandl quandl = new Quandl();
	private Sina sina = new Sina();
	private Yahoo yahoo = new Yahoo();

	private Src srcHkd__ = new Src(hkd::quote, hkd::dataSource);
	private Src srcHkex_ = new Src(sina::quote, yahoo::dataSourceL1);
	private Src srcIndex = new Src(yahoo::quote, yahoo::dataSourceL1);
	private Src srcNymex = new Src(yahoo::quote, quandl::dataSourceCsv);
	private Src srcNone_ = new Src(google::quote, null);

	private class Src {
		private Fun<Set<String>, Map<String, Float>> quoteFun;
		private Fun2<String, TimeRange, DataSource> dataSourceFun;

		private Src( //
				Fun<Set<String>, Map<String, Float>> quoteFun, //
				Fun2<String, TimeRange, DataSource> dataSourceFun) {
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

	public Streamlet<Asset> queryCompanies() {
		return hkex.queryCompanies().filter(this::filter);
	}

	public Asset queryCompany(String symbol) {
		return filter(symbol) ? hkex.queryCompany(symbol) : null;
	}

	public Streamlet<Asset> queryCompaniesByMarketCap(Time time) {
		int year = time.year() - 1;
		return Read.from(hkexFactBook.queryLeadingCompaniesByMarketCap(year)) //
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
		Map<Fun<Set<String>, Map<String, Float>>, Set<String>> map = new HashMap<>();

		for (String symbol : symbols)
			if (filter(symbol))
				map.computeIfAbsent(src(symbol).quoteFun, s -> new HashSet<>()).add(symbol);

		return Read //
				.from2(map) //
				.concatMap2((quoteFun, symbols_) -> Read.from2(quoteFun.apply(symbols_))) //
				.toMap();
	}

	private DataSource dataSource_(String symbol, TimeRange period) {
		return src(symbol).dataSourceFun.apply(symbol, period);
	}

	private Src src(String symbol) {
		if (String_.equals(symbol, Asset.cashSymbol))
			return srcHkd__;
		else if (symbol.endsWith(".HK"))
			return srcHkex_;
		else if (symbol.startsWith("^") || String_.equals(symbol, "NDAQ"))
			return srcIndex;
		else if (String_.equals(symbol, "CL=F") || symbol.endsWith(".NYM"))
			return srcNymex;
		else if (Boolean.FALSE)
			return srcNone_;
		else
			throw new RuntimeException(symbol);
	}

	private boolean filter(Asset asset) {
		return filter(asset.symbol);
	}

	private boolean filter(String symbol) {
		return !Trade_.blackList.contains(symbol);
	}

}
