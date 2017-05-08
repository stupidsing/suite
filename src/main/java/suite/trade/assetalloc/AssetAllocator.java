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
	 * @return a portfolio consisting of list of stock codes and potential
	 *         values, or null if the strategy do not want to trade on that
	 *         date. The assets will be allocated according to potential values
	 *         pro-rata.
	 */
	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceByStockCode, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate);

}
