package suite;

import org.telegram.telegrambots.ApiContextInitializer;

import suite.http.HttpServerMain;
import suite.telegram.TelegramBot;
import suite.util.Thread_;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.ScheduleMain
public class ServerMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(ServerMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Thread_.startThread(() -> HttpServerMain.main(args));
		Thread_.startThread(() -> ScheduleMain.main(args));
		Thread_.startThread(() -> {
			ApiContextInitializer.init();
			new TelegramBot().bot((userId, message) -> message);
		});
		return true;
	}

}
