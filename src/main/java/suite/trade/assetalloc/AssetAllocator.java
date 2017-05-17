package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.trade.data.DataSource;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface AssetAllocator {

	/**
	 * @return a portfolio consisting of list of symbols and potential values,
	 *         or null if the strategy do not want to trade on that date. The
	 *         assets will be allocated according to potential values pro-rata.
	 */
	public OnDate allocate(Map<String, DataSource> dataSourceBySymbol, List<LocalDate> tradeDates);

	public interface OnDate {
		public List<Pair<String, Double>> onDate(LocalDate backTestDate);
	}

}
