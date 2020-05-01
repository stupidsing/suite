package suite.trade.data;

import primal.fp.Funs.Source;
import primal.streamlet.Streamlet;
import suite.cfg.Defaults;
import suite.cfg.HomeDir;
import suite.http.HttpClient;
import suite.streamlet.As;
import suite.streamlet.ReadBytes;
import suite.trade.Trade;
import suite.util.Memoize;

import java.nio.file.Files;

import static java.lang.Math.max;
import static java.lang.Math.min;

public interface Broker {

	public Streamlet<Trade> queryHistory();

	public double transactionFee(double transactionAmount);

	// https://www.personal.hsbc.com.hk/1/2/hk/investments/stocks/detail
	public class Hsbc implements Broker { // bloodsucker
		public Streamlet<Trade> queryHistory() {
			return memoizeHistoryRecords.g();
		}

		private static Source<Streamlet<Trade>> memoizeHistoryRecords = Memoize.source(Hsbc::queryHistory_);

		private static Streamlet<Trade> queryHistory_() {
			var path = HomeDir.resolve("home-data").resolve("stock.txt");

			var bytes = Files.exists(path)
					? ReadBytes.from(path)
					: Defaults.bindSecrets("stockUrl .0").map(url -> new Streamlet<>(HttpClient.get(url)::out));

			return bytes.collect(As::table).map(Trade::of).collect();
		}

		public double dividendFee(double dividendAmount) {
			return min(30d, dividendAmount * .01d * .5d);
		}

		public double transactionFee(double transactionAmount) {

			// .15d during promotion period
			var hsbcInternetBanking = max(transactionAmount * .01d * .25d, 100d);

			var stampDuty = transactionAmount * .01d * .1d;
			var sfcTxLevy = transactionAmount * .01d * .0027d;
			var sfcInvestorLevy = transactionAmount * .01d * .002d; // suspended
			var hkex = transactionAmount * .01d * .005d;

			// TODO deposit transaction charge (for purchase transaction only)
			// HKD5/RMB5 per board lot (minimum charge: HKD30/RMB30, maximum
			// charge: HKD200/RMB200), waived if the same stocks are purchased
			// and then sold on the same trading day or the subsequent trading
			// day (T or T+1)

			return hsbcInternetBanking + stampDuty + sfcTxLevy + 0d * sfcInvestorLevy + hkex;
		}
	}

}
