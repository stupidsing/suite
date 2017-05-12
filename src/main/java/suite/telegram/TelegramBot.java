package suite.telegram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import suite.util.Rethrow;
import suite.util.TempDir;
import suite.util.Thread_;

public class TelegramBot {

	public void bot(BiFunction<Integer, String, String> fun) {
		ApiContextInitializer.init();

		try {
			new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return "Kowloonbot";
				}

				public String getBotToken() {
					Path path = TempDir.resolve("kowloonbot.token");
					return Rethrow.ex(() -> Files.readAllLines(path)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage()) {
						Message message = update.getMessage();

						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(message.getChat().getId().toString());
						sendMessage.setText(fun.apply(message.getFrom().getId(), message.getText()));

						try {
							sendMessage(sendMessage);
						} catch (TelegramApiException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
			});
		} catch (TelegramApiException ex) {
			throw new RuntimeException(ex);
		}

		while (true)
			Thread_.sleepQuietly(10000);
	}
}
