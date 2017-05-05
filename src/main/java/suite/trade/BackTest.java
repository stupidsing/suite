package suite.trade;

import suite.algo.Statistic;
import suite.trade.Strategy.GetBuySell;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class BackTest {

	public final Account account = new Account();
	public final StringBuilder tradeLog = new StringBuilder();
	public final StringBuilder concludeLog = new StringBuilder();

	private Sink<String> tradeLogSink = To.sink(tradeLog);
	private Sink<String> concludeLogSink = concludeLog::append;

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
		buySell(ds, length - 1, -account.nShares());

		float return_ = account.cash();
		float nApproxYears = ds.nYears();
		double sharpe = return_ / (Math.sqrt(nApproxYears * new Statistic().variance(valuations)));
		// new TimeSeries().sharpeRatio(valuations, nApproxYears);

		concludeLogSink.sink("" //
				+ ", nYears = " + To.string(nApproxYears) //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", return = " + To.string(return_) //
				+ ", sharpe = " + To.string(sharpe));
	}

	private float buySell(DataSource ds, int day, int buySell) {
		float price = ds.prices[day];
		account.buySell(buySell, price);
		float valuation = account.valuation(To.map(Account.defaultStockCode, price));

		if (day == 0 || buySell != 0)
			tradeLogSink.sink("" //
					+ "> date = " + ds.dates[day] //
					+ ", buy/sell = " + buySell //
					+ ", price = " + To.string(price) //
					+ ", nShares = " + account.nShares() //
					+ ", valuation = " + To.string(valuation));

		return valuation;
	}

}
