package suite;

import java.time.LocalDate;

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
	protected boolean run(String[] args) throws Exception {
		Strategy strategy = new Strategos().movingAvgMeanReverting(128, 8, 0.15f);
		LocalDate frDate = LocalDate.of(2013, 1, 1);
		LocalDate toDate = LocalDate.of(2018, 1, 1);
		StringBuilder sb = new StringBuilder();

		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex) {
			String stockCode = stock.t0 + ".HK";
			String stockName = stock.t1;

			String prefix = stockCode + " " + stockName;
			try {
				DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
				float prices[] = source.prices;

				int signal = strategy.analyze(prices).get(prices.length - 1);
				if (signal != 0)
					sb.append("equity " + stockCode + " " + stock.t1 + " has signal " + signal);

				Util.sleepQuietly(2000);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + prefix);
			}
		}

		if (0 < sb.length())
			new SmtpSslGmail().send(null, getClass().getName(), sb.toString());

		return true;
	}

}
