package suite;

import suite.trade.data.Configuration;
import suite.trade.data.Summarize;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.StatusMain
public class StatusMain extends ExecutableProgram {

	private Configuration configuration = new Configuration();

	public static void main(String[] args) {
		Util.run(StatusMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Summarize summarize = new Summarize(configuration);
		System.out.println(summarize.summarize(r -> r.strategy, s -> {
		}));
		return true;
	}

}
