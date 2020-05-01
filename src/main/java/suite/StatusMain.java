package suite;

import suite.trade.analysis.Summarize;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.util.RunUtil;

import java.util.TreeMap;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain {

	private TradeCfg cfg = new TradeCfgImpl();

	public static void main(String[] args) {
		RunUtil.run(new StatusMain()::run);
	}

	private boolean run() {
		var summarize = Summarize.of(cfg);
		var sbs = summarize.summarize(trade -> trade.strategy);
		System.out.println(sbs.log);
		System.out.println(new TreeMap<>(sbs.pnlByKey));
		return true;
	}

}
