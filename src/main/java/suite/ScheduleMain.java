package suite;

import suite.os.Scheduler;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ScheduleMain
public class ScheduleMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(ScheduleMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		new Scheduler().run();
		return true;
	}

}
