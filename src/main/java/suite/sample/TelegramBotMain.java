package suite.sample;

import java.nio.file.Path;

import org.telegram.telegrambots.ApiContextInitializer;

import suite.telegram.TelegramBot;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.sample.TelegramBotMain Kowloonbot kowloonbot.token
public class TelegramBotMain {

	private TelegramBot tb;

	public static void main(String[] args) {
		var botUsername = args[0];
		var tokenPath = Path.of(args[1]);
		RunUtil.run(new TelegramBotMain(botUsername, tokenPath)::run);
	}

	public TelegramBotMain(String botUsername, Path tokenPath) {
		tb = new TelegramBot(botUsername, tokenPath);
	}

	public boolean run() {
		ApiContextInitializer.init();
		tb.bot((userId, message) -> message);
		return true;
	}

}
