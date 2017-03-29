package suite.trade;

import suite.os.LogUtil;
import suite.trade.Strategy.GetBuySell;

public class BackTest {

	public final Account account = new Account();

	public BackTest(DataSource source, Strategy strategy) {
		double prices[] = source.prices;

		GetBuySell getBuySell = strategy.analyze(prices);

		for (int day = 0; day < prices.length; day++) {
			int buySell = getBuySell.get(day);
			double price = prices[day];

			account.buySell(buySell, price);

			if (buySell != 0)
				LogUtil.info("" //
						+ "date = " + source.dates[day] //
						+ ", price = " + price //
						+ ", buy/sell = " + buySell //
						+ ", nLots = " + account.nLots());

			if (Boolean.FALSE) // do not validate yet
				account.validate();
		}

		// sell all stocks at the end
		account.buySell(-account.nLots(), prices[prices.length - 1]);
	}

}
