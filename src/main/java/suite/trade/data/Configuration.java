package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;

public interface Configuration {

	public DataSource dataSource(String symbol);

	public DataSource dataSource(String symbol, TimeRange period);

	public Streamlet<Asset> queryCompanies();

	public Streamlet<Asset> queryCompaniesByMarketCap(Time time);

	public Asset queryCompany(String symbol);

	public Streamlet<Trade> queryHistory();

	public Map<String, Float> quote(Set<String> symbols);

	public double transactionFee(double transactionAmount);

}
