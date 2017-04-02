package suite.trade;

import suite.os.LogUtil;
import suite.trade.Strategy.GetBuySell;

public class BackTest {

	public final Account account = new Account();

	public static BackTest test(DataSource source, Strategy strategy) {
		return new BackTest(source, strategy);
	}

	private BackTest(DataSource source, Strategy strategy) {
		float prices[] = source.prices;

		GetBuySell getBuySell = strategy.analyze(prices);

		for (int day = 0; day < prices.length; day++) {
			int buySell = getBuySell.get(day);

			buySell(source, day, buySell);

			if (Boolean.FALSE) // do not validate yet
				account.validate();
		}

		// sell all stocks at the end
		buySell(source, prices.length - 1, -account.nLots());
	}

	private void buySell(DataSource source, int day, int buySell) {
		float price = source.prices[day];
		account.buySell(buySell, price);

		if (buySell != 0) {
			float valuation = account.cash() + account.nLots() * price;

			LogUtil.info("" //
					+ "date = " + source.dates[day] //
					+ ", price = " + String.format("%.2f", price) //
					+ ", valuation = " + String.format("%.2f", valuation) //
					+ ", buy/sell = " + buySell //
					+ ", nLots = " + account.nLots());
		}
	}

}
