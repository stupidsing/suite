package suite.telegram;

import java.nio.file.Files;

import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import suite.Defaults;
import suite.util.Fail;
import suite.util.FunUtil2.FoldOp;
import suite.util.Rethrow;
import suite.util.Thread_;

public class TelegramBot {

	public void bot(FoldOp<Integer, String> fun) {
		try {
			new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return "Kowloonbot";
				}

				public String getBotToken() {
					var path = Defaults.tmp("kowloonbot.token");
					return Rethrow.ex(() -> Files.readAllLines(path)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage()) {
						var message = update.getMessage();

						var sendMessage = new SendMessage();
						sendMessage.setChatId(message.getChat().getId().toString());
						sendMessage.setText(fun.apply(message.getFrom().getId(), message.getText()));

						try {
							sendApiMethod(sendMessage);
						} catch (TelegramApiException ex) {
							Fail.t(ex);
						}
					}
				}
			});
		} catch (TelegramApiException ex) {
			Fail.t(ex);
		}

		while (true)
			Thread_.sleepQuietly(10000);
	}
}
