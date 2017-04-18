package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.BackTest;
import suite.trade.DataSource;
import suite.trade.Hkex;
import suite.trade.Hkex.Company;
import suite.trade.Strategos;
import suite.trade.Strategy;
import suite.trade.Yahoo;
import suite.util.Serialize;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String args[]) {
		Yahoo yahoo = new Yahoo();
		Streamlet<Company> companies = new Hkex().companies;
		Strategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);

		Map<String, Boolean> backTestByStockCode = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get("backTestByStockCode", () -> companies //
						.map2(stock -> stock.code, stock -> {
							try {
								DataSource ds = yahoo.dataSource(stock.code);
								BackTest backTest = BackTest.test(ds, strategy);
								return 0f < backTest.account.cash();
							} catch (Exception ex) {
								LogUtil.warn(ex.getMessage() + " in " + stock);
								return false;
							}
						}) //
						.toMap());

		LocalDate today = LocalDate.now();
		LocalDate frDate = today.minusDays(128);
		LocalDate toDate = today;
		List<String> messages = new ArrayList<>();

		for (Company company : companies) {
			String stockCode = company.code;

			if (backTestByStockCode.get(stockCode)) {
				String prefix = company.toString();

				try {
					DataSource ds = yahoo.dataSource(stockCode, frDate, toDate);
					float[] prices = ds.prices;

					int signal = strategy.analyze(prices).get(prices.length - 1);
					String message = "equity " + stockCode + " " + company.name + " has signal " + signal;
					LogUtil.info(message);
					if (signal != 0)
						messages.add(message);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + prefix);
				}
			}
		}

		String result = Read.from(messages).collect(As.joined("\n"));

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);

		return true;
	}

}
