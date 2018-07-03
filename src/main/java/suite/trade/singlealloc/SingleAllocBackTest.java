package suite.trade.singlealloc;

import static suite.util.Friends.sqrt;

import suite.math.numeric.Statistic;
import suite.streamlet.FunUtil.Sink;
import suite.trade.Account;
import suite.trade.Time;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.util.To;

public class SingleAllocBackTest {

	public final Account account = Account.ofCash(0f);
	public final StringBuilder tradeLog = new StringBuilder();
	public final StringBuilder concludeLog = new StringBuilder();

	private Statistic stat = new Statistic();
	private String symbol = "-";
	private Sink<String> tradeLogSink = To.sink(tradeLog);
	private Sink<String> concludeLogSink = concludeLog::append;

	public static SingleAllocBackTest test(DataSource ds, BuySellStrategy strategy) {
		return new SingleAllocBackTest(ds, strategy);
	}

	private SingleAllocBackTest(DataSource ds, BuySellStrategy strategy) {
		var prices = ds.prices;
		var length = prices.length;
		var valuations = new float[length];

		var getBuySell = strategy.analyze(prices);

		for (var day = 0; day < length; day++) {
			var buySell = getBuySell.get(day);
			valuations[day] = buySell(ds, day, buySell);

			if (Boolean.FALSE) // do not validate yet
				account.validate();
		}

		// sell all stocks at the end
		buySell(ds, length - 1, -account.nShares(symbol));

		var return_ = account.cash();
		var sharpe = return_ / (sqrt(stat.variance(valuations) * Trade_.nTradeDaysPerYear / length));
		// new TimeSeries().sharpeRatio(valuations, nApproxYears);

		concludeLogSink.sink("" //
				+ ", " + account.transactionSummary(a -> 0d) //
				+ ", return = " + To.string(return_) //
				+ ", sharpe = " + To.string(sharpe));
	}

	private float buySell(DataSource ds, int day, int buySell) {
		var price = ds.prices[day];
		account.play(Trade.of(buySell, symbol, price), false);
		var valuation = account.valuation(symbol -> price).sum();

		if (day == 0 || buySell != 0)
			tradeLogSink.sink("" //
					+ "> date = " + Time.ofEpochSec(ds.ts[day]).ymd() //
					+ ", buy/sell = " + buySell //
					+ ", price = " + To.string(price) //
					+ ", nShares = " + account.nShares(symbol) //
					+ ", valuation = " + To.string(valuation));

		return valuation;
	}

}
