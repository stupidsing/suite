package suite.telegram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import suite.util.Rethrow;

public class TelegramTest {

	@Test
	public void testBot() {
		try {
			new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return "kowloonbot";
				}

				public String getBotToken() {
					Path path = Paths.get("/Users/ywsing/kowloonbot.token");
					return Rethrow.ioException(() -> Files.readAllLines(path)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage())
						System.out.println(update.getMessage().getText());
				}
			});
		} catch (TelegramApiException ex) {
			throw new RuntimeException(ex);
		}
	}

}
