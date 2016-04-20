package suite.telegram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import suite.os.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

public class TelegramBot {

	public void bot(Fun<String, String> fun) {
		try {
			new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return "Kowloonbot";
				}

				public String getBotToken() {
					Path path = Paths.get(FileUtil.tmp + "/kowloonbot.token");
					return Rethrow.ioException(() -> Files.readAllLines(path)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage()) {
						Message message = update.getMessage();

						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(message.getChat().getId().toString());
						sendMessage.setText(fun.apply(message.getText()));

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
			Util.sleepQuietly(10000);
	}
}
