package suite.trade.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.data.Broker.Hsbc;
import suite.util.To;
import suite.util.Util;

public class Configuration {

	private Broker broker = new Hsbc();
	private Hkd hkd = new Hkd();
	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private Yahoo yahoo = new Yahoo();

	public DataSource dataSource(String symbol) {
		return yahoo.dataSource(symbol);
	}

	public DataSource dataSource(String symbol, DatePeriod period) {
		return yahoo.dataSource(symbol, period);
	}

	public DataSource dataSourceWithLatestQuote(String symbol) {
		return yahoo.dataSourceWithLatestQuote(symbol);
	}

	public Asset getCompany(String code) {
		return hkex.getCompany(code);
	}

	public Streamlet<Asset> getCompanies() {
		return hkex.getCompanies();
	}

	public Streamlet<Asset> queryCompanies() {
		return hkex.queryCompanies();
	}

	public Streamlet<Asset> queryLeadingCompaniesByMarketCap(int year) {
		return hkexFactBook.queryLeadingCompaniesByMarketCap(year);
	}

	public Map<String, Integer> queryLotSizeBySymbol(Streamlet<Asset> assets) {
		return hkex.queryLotSizeBySymbol(assets);
	}

	public Map<String, Float> quote(Set<String> symbols) {
		Set<String> hkdCodes = new HashSet<>();
		Set<String> yahooSymbols = new HashSet<>();

		for (String symbol : symbols) {
			Set<String> set;
			if (Util.stringEquals(symbol, Asset.cashCode))
				set = hkdCodes;
			else
				set = yahooSymbols;
			set.add(symbol);
		}

		return To.map_(hkd.quote(hkdCodes), yahoo.quote(yahooSymbols));
	}

	public double transactionFee(double transactionAmount) {
		return broker.transactionFee(transactionAmount);
	}

}
