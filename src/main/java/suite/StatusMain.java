package suite;

import java.util.TreeMap;

import suite.trade.analysis.Summarize;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain extends ExecutableProgram {

	private TradeCfg cfg = new TradeCfgImpl();

	public static void main(String[] args) {
		RunUtil.run(StatusMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		var summarize = Summarize.of(cfg);
		var sbs = summarize.summarize(r -> r.strategy);
		System.out.println(sbs.log);
		System.out.println(new TreeMap<>(sbs.pnlByKey));
		return true;
	}

}
