package suite.trade;

import java.time.LocalDate;

import suite.algo.Statistic;
import suite.math.MathUtil;
import suite.trade.Strategy.GetBuySell;
import suite.util.FormatUtil;

public class BackTest {

	public final Account account = new Account();
	public final StringBuilder tradeLog = new StringBuilder();
	public final StringBuilder concludeLog = new StringBuilder();

	public static BackTest test(DataSource ds, Strategy strategy) {
		return new BackTest(ds, strategy);
	}

	private BackTest(DataSource ds, Strategy strategy) {
		float[] prices = ds.prices;
		int length = prices.length;
		float[] valuations = new float[length];

		GetBuySell getBuySell = strategy.analyze(prices);

		for (int day = 0; day < length; day++) {
			int buySell = getBuySell.get(day);
			valuations[day] = buySell(ds, day, buySell);

			if (Boolean.FALSE) // do not validate yet
				account.validate();
		}

		// sell all stocks at the end
		buySell(ds, length - 1, -account.nLots());

		float return_ = account.cash();
		LocalDate dateStart = LocalDate.parse(ds.dates[0], FormatUtil.dateFormat);
		LocalDate dateEnd = LocalDate.parse(ds.dates[length - 1], FormatUtil.dateFormat);
		long nApproxYears = (dateEnd.toEpochDay() - dateStart.toEpochDay()) / 365l;
		double sharpe = return_ / (Math.sqrt(nApproxYears) * new Statistic().standardDeviation(valuations));

		concludeLog.append("" //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", return = " + MathUtil.format(return_) //
				+ ", sharpe = " + MathUtil.format(sharpe));
	}

	private float buySell(DataSource ds, int day, int buySell) {
		float price = ds.prices[day];
		account.buySell(buySell, price);
		float valuation = account.cash() + account.nLots() * price;

		if (day == 0 || buySell != 0)
			tradeLog.append("\n" //
					+ "date = " + ds.dates[day] //
					+ ", buy/sell = " + buySell //
					+ ", price = " + MathUtil.format(price) //
					+ ", nLots = " + account.nLots() //
					+ ", valuation = " + MathUtil.format(valuation));

		return valuation;
	}

}
