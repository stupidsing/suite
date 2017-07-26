package suite;

import java.util.TreeMap;

import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain extends ExecutableProgram {

	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		Util.run(StatusMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Summarize summarize = Summarize.of(cfg);
		SummarizeByStrategy<String> sbs = summarize.summarize(r -> r.strategy);
		System.out.println(sbs.log);
		System.out.println(new TreeMap<>(sbs.pnlByKey));

		for (String symbol : sbs.overall.assets().keySet()) {
			float[] closes = cfg.dataSource(symbol).closes;
			float close_ = closes[closes.length - 1];
			Float price = summarize.priceBySymbol.get(symbol);
			if (price != null)
				System.out.println(cfg.queryCompany(symbol) //
						+ " (" + (price < close_ ? "" : "+") + To.string((price - close_) / close_) + ")" //
						+ ": " + To.string(close_) //
						+ " -> " + To.string(price));
		}

		return true;
	}

}
