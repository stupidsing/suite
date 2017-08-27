package suite.trade.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.IntIntSink;
import suite.primitive.Ints_;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Trade;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Memoize;
import suite.util.String_;

public interface Broker {

	public Streamlet<Trade> queryHistory();

	public double transactionFee(double transactionAmount);

	// https://www.personal.hsbc.com.hk/1/2/hk/investments/stocks/detail
	public class Hsbc implements Broker {
		public Streamlet<Trade> queryHistory() {
			return memoizeHistoryRecords.source();
		}

		private static Source<Streamlet<Trade>> memoizeHistoryRecords = Memoize.source(Hsbc::queryHistory_);

		private static Streamlet<Trade> queryHistory_() {
			String url = "https://raw.githubusercontent.com/stupidsing/home-data/master/stock.txt";
			Path path = HomeDir.resolve("workspace").resolve("home-data").resolve("stock.txt");
			Streamlet<Bytes> bytes = Files.exists(path) ? Read.bytes(path) : Read.url(url);

			Trade[] trades0 = bytes.collect(As::table).map(Trade::of).toArray(Trade.class);
			List<Trade> trades1 = new ArrayList<>();
			int length0 = trades0.length;
			int i0 = 0;

			IntIntSink tx = (i0_, i1_) -> {
				if (Ints_.range(i0_, i1_).mapInt(i -> trades0[i].buySell).sum() != 0)
					while (i0_ < i1_)
						trades1.add(trades0[i0_++]);
			};

			for (int i = 1; i < length0; i++) {
				Trade trade0 = trades0[i0];
				Trade trade1 = trades0[i];
				boolean isGroup = true //
						&& String_.equals(trade0.date, trade1.date) //
						&& String_.equals(trade0.symbol, trade1.symbol) //
						&& trade0.price == trade1.price;
				if (!isGroup) {
					tx.sink2(i0, i);
					i0 = i;
				}
			}

			tx.sink2(i0, length0);
			return Read.from(trades1);
		}

		public double transactionFee(double transactionAmount) {

			// .15d during promotion period
			double hsbcInternetBanking = Math.min(transactionAmount * .01d * .25d, 100d);

			double stampDuty = transactionAmount * .01d * .1d;
			double sfcTxLevy = transactionAmount * .01d * .0027d;
			double sfcInvestorLevy = transactionAmount * .01d * .002d; // suspended
			double hkex = transactionAmount * .01d * .005d;

			// TODO deposit transaction charge (for purchase transaction only)
			// HKD5/RMB5 per board lot (minimum charge: HKD30/RMB30, maximum
			// charge: HKD200/RMB200), waived if the same stocks are purchased
			// and then sold on the same trading day or the subsequent trading
			// day (T or T+1)

			return hsbcInternetBanking + stampDuty + sfcTxLevy + 0d * sfcInvestorLevy + hkex;
		}
	}

}
