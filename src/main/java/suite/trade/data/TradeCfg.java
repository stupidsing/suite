package suite.trade.data;

import java.util.Map;
import java.util.Set;

import suite.os.LogUtil;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.data.DataSource.AlignKeyDataSource;

public interface TradeCfg {

	public DataSource dataSource(String symbol);

	public DataSource dataSource(String symbol, TimeRange period);

	public Streamlet<Asset> queryCompanies();

	public Streamlet<Asset> queryCompaniesByMarketCap(Time time);

	public Asset queryCompany(String symbol);

	public Streamlet<Trade> queryHistory();

	public Map<String, Float> quote(Set<String> symbols);

	public double transactionFee(double transactionAmount);

	public default AlignKeyDataSource<String> dataSources(TimeRange period, Streamlet<String> symbols) {
		return symbols //
				.map2(symbol -> {
					try {
						return dataSource(symbol, period).validate();
					} catch (Exception ex) {
						LogUtil.warn("for " + symbol + " " + ex);
						return null;
					}
				}) //
				.filterValue(ds -> ds != null) //
				.collect() //
				.apply(DataSource::alignAll);
	}

}
