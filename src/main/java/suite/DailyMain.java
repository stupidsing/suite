package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.os.LogUtil;
import suite.smtp.SmtpSslGmail;
import suite.trade.DataSource;
import suite.trade.Hkex;
import suite.trade.Strategos;
import suite.trade.Strategy;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String args[]) throws Exception {
		Strategy strategy = new Strategos().movingAvgMeanReverting(128, 8, 0.15f);
		LocalDate today = LocalDate.now();
		LocalDate frDate = today.minusDays(128);
		LocalDate toDate = today;
		List<String> results = new ArrayList<>();

		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex) {
			String stockCode = stock.t0 + ".HK";
			String stockName = stock.t1;

			String prefix = stockCode + " " + stockName;
			try {
				DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
				float prices[] = source.prices;

				int signal = strategy.analyze(prices).get(prices.length - 1);
				if (signal != 0)
					results.add("\nequity " + stockCode + " " + stock.t1 + " has signal " + signal);

				Util.sleepQuietly(2000);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + prefix);
			}
		}

		SmtpSslGmail smtp = new SmtpSslGmail();

		for (String result : results) {
			LogUtil.info(result);
			smtp.send(null, getClass().getName(), result);
		}

		return true;
	}

}
