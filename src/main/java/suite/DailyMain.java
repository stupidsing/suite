package suite;

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
import suite.trade.Period;
import suite.trade.Strategos;
import suite.trade.Strategy;
import suite.trade.Yahoo;
import suite.util.Serialize;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Hkex hkex = new Hkex();
		Yahoo yahoo = new Yahoo();
		Streamlet<Company> companies = hkex.companies;
		Strategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);

		Map<String, Boolean> backTestByStockCode = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get("backTestByStockCode", () -> companies //
						.map2(stock -> stock.code, stock -> {
							try {
								DataSource ds = yahoo.dataSource(stock.code, Period.fiveYears());
								BackTest backTest = BackTest.test(ds, strategy);
								return 0f < backTest.account.cash();
							} catch (Exception ex) {
								LogUtil.warn(ex.getMessage() + " in " + stock);
								return false;
							}
						}) //
						.toMap());

		Period period = Period.beforeToday(128);
		List<String> messages = new ArrayList<>();

		for (Company company : companies) {
			String stockCode = company.code;

			if (backTestByStockCode.get(stockCode)) {
				String prefix = company.toString();

				try {
					DataSource ds = yahoo.dataSource(stockCode, period);
					float[] prices = ds.prices;
					int last = prices.length - 1;
					int signal = strategy.analyze(prices).get(last);
					String message = company + " has signal " + signal + " for price " + prices[last];

					if (signal != 0) {
						LogUtil.info(message);
						messages.add(message);
					}
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
