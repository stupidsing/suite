package suite.trade.backalloc;

import java.time.LocalDateTime;
import java.util.List;

import suite.adt.pair.Pair;
import suite.streamlet.Streamlet2;
import suite.trade.data.DataSource;

/**
 * Strategy that advise you how to divide your money into different investments,
 * i.e. set up a portfolio.
 *
 * @author ywsing
 */
public interface BackAllocator {

	public OnDateTime allocate(Streamlet2<String, DataSource> dataSourceBySymbol, List<LocalDateTime> dts);

	public interface OnDateTime {

		/**
		 * @return a portfolio consisting of list of symbols and potential
		 *         values, or null if the strategy do not want to trade on that
		 *         date. The assets will be allocated according to potential
		 *         values pro-rata.
		 */
		public List<Pair<String, Double>> onDate(LocalDateTime backTestDt, int index);
	}

}
