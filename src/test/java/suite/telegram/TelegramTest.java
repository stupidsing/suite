package suite.telegram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import suite.os.FileUtil;
import suite.util.Rethrow;

public class TelegramTest {

	@Test
	public void testBot() {
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
						Chat chat = message.getChat();

						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(chat.getId().toString());
						sendMessage.setText(message.getText());

						try {
							sendMessage(sendMessage);
						} catch (TelegramApiException ex) {
							throw new RuntimeException(ex);
						}

						System.out.println(message.getText());
					}
				}
			});
		} catch (TelegramApiException ex) {
			throw new RuntimeException(ex);
		}
	}

}
