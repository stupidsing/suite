package suite.trade.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.os.SerializedStoreCache;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.data.Broker.Hsbc;
import suite.util.FormatUtil;
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

		// count as tomorrow open if market is closed (after 4pm)
		LocalDate tradeDate = LocalDateTime.now().plusHours(8).toLocalDate();
		String date = FormatUtil.formatDate(tradeDate);

		return SerializedStoreCache //
				.of(DataSource.serializer) //
				.get(getClass().getSimpleName() + ".dataSourceWithLatestQuote(" + symbol + ", " + date + ")", () -> {
					float price = quote(Collections.singleton(symbol)).get(symbol);
					return dataSource(symbol, DatePeriod.ages()).cons(date, price);
				});
	}

	public Streamlet<Asset> getCompanies() {
		return hkex.getCompanies();
	}

	public Asset queryCompany(String symbol) {
		return hkex.queryCompany(symbol);
	}

	public Streamlet<Asset> queryCompanies() {
		return hkex.queryCompanies();
	}

	public Streamlet<Asset> queryLeadingCompaniesByMarketCap(int year) {
		return Read.from(hkexFactBook.queryLeadingCompaniesByMarketCap(year)).map(this::queryCompany);
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
