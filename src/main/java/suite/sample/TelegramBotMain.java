package suite.sample;

import org.telegram.telegrambots.ApiContextInitializer;
import suite.os.Execute;
import suite.telegram.TelegramBot;
import suite.util.RunUtil;

import java.nio.file.Path;

// mvn compile exec:java -Dexec.mainClass=suite.sample.TelegramBotMain Kowloonbot kowloonbot.token
public class TelegramBotMain {

	private TelegramBot tb;

	public static void main(String[] args) {
		var botUsername = args[0];
		var tokenPath = Path.of(args[1]);
		RunUtil.run(new TelegramBotMain(botUsername, tokenPath)::run);
	}

	public TelegramBotMain(String botUsername, Path tokenPath) {
		tb = new TelegramBot(botUsername, tokenPath,
				token -> true,
				() -> "time is " + System.currentTimeMillis(),
				(chatId, do_) -> Execute.shell(System.getenv("DO_" + do_.toUpperCase())));
	}

	public boolean run() {
		ApiContextInitializer.init();
		tb.bot();
		return true;
	}

}
