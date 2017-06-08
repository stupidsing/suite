package suite;

import java.util.TreeMap;

import suite.trade.analysis.Summarize;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
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
		System.out.println(new TreeMap<>(summarize.out(System.out::println, r -> r.strategy)));
		return true;
	}

}
