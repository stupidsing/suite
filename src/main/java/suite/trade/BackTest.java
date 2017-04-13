package suite.trade;

import suite.trade.Strategy.GetBuySell;

public class BackTest {

	public final Account account = new Account();
	public final StringBuilder log = new StringBuilder();

	public static BackTest test(DataSource ds, Strategy strategy) {
		return new BackTest(ds, strategy);
	}

	private BackTest(DataSource ds, Strategy strategy) {
		float[] prices = ds.prices;

		GetBuySell getBuySell = strategy.analyze(prices);

		for (int day = 0; day < prices.length; day++) {
			int buySell = getBuySell.get(day);

			buySell(ds, day, buySell);

			if (Boolean.FALSE) // do not validate yet
				account.validate();
		}

		// sell all stocks at the end
		buySell(ds, prices.length - 1, -account.nLots());
	}

	private void buySell(DataSource ds, int day, int buySell) {
		float price = ds.prices[day];
		account.buySell(buySell, price);

		if (day == 0 || buySell != 0) {
			float valuation = account.cash() + account.nLots() * price;

			log.append("\n" //
					+ "date = " + ds.dates[day] //
					+ ", buy/sell = " + buySell //
					+ ", price = " + String.format("%.2f", price) //
					+ ", nLots = " + account.nLots() //
					+ ", valuation = " + String.format("%.2f", valuation));
		}
	}

}
