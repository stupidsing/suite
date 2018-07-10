package suite.sample;

import org.telegram.telegrambots.ApiContextInitializer;

import suite.telegram.TelegramBot;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

public class TelegramBotMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(TelegramBotMain.class, args);
	}

	protected boolean run(String[] args) {
		ApiContextInitializer.init();
		new TelegramBot().bot((userId, message) -> message);
		return true;
	}

}
