package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import suite.os.LogUtil;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.DataSource;
import suite.trade.Hkex;
import suite.trade.Hkex.Company;
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
	protected boolean run(String args[]) {
		Strategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);
		LocalDate today = LocalDate.now();
		LocalDate frDate = today.minusDays(128);
		LocalDate toDate = today;
		List<String> messages = new ArrayList<>();

		for (Company company : new Hkex().companies) {
			String stockCode = company.code + ".HK";
			String stockName = company.name;
			String prefix = stockCode + " " + stockName;

			try {
				DataSource ds = DataSource.yahoo(stockCode, frDate, toDate);
				float prices[] = ds.prices;

				int signal = strategy.analyze(prices).get(prices.length - 1);
				if (signal != 0)
					messages.add("equity " + stockCode + " " + company.name + " has signal " + signal);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + prefix);
			}

			Util.sleepQuietly(2000);
		}

		String result = Read.from(messages).collect(As.joined("\n"));
		LogUtil.info(result);

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);

		return true;
	}

}
