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

	public DataSource dataSource(String stockCode) {
		return yahoo.dataSource(stockCode);
	}

	public DataSource dataSource(String stockCode, DatePeriod period) {
		return yahoo.dataSource(stockCode, period);
	}

	public DataSource dataSourceWithLatestQuote(String stockCode) {
		return yahoo.dataSourceWithLatestQuote(stockCode);
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

	public Map<String, Integer> queryLotSizeByStockCode(Streamlet<Asset> assets) {
		return hkex.queryLotSizeByStockCode(assets);
	}

	public Map<String, Float> quote(Set<String> stockCodes) {
		Set<String> hkdCodes = new HashSet<>();
		Set<String> yahooStockCodes = new HashSet<>();

		for (String stockCode : stockCodes) {
			Set<String> set;
			if (Util.stringEquals(stockCode, Asset.cashCode))
				set = hkdCodes;
			else
				set = yahooStockCodes;
			set.add(stockCode);
		}

		return To.map_(hkd.quote(hkdCodes), yahoo.quote(yahooStockCodes));
	}

	public double transactionFee(double transactionAmount) {
		return broker.transactionFee(transactionAmount);
	}

}
