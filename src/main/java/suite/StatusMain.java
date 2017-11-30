package suite;

import java.util.TreeMap;

import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain extends ExecutableProgram {

	private Configuration cfg = new ConfigurationImpl();

	public static void main(String[] args) {
		RunUtil.run(StatusMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Summarize summarize = Summarize.of(cfg);
		SummarizeByStrategy<String> sbs = summarize.summarize(r -> r.strategy);
		System.out.println(sbs.log);
		System.out.println(new TreeMap<>(sbs.pnlByKey));
		return true;
	}

}
