package suite.sample;

import org.telegram.telegrambots.ApiContextInitializer;

import suite.telegram.TelegramBot;
import suite.util.RunUtil;

public class TelegramBotMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			ApiContextInitializer.init();
			new TelegramBot().bot((userId, message) -> message);
			return true;
		});
	}

}
