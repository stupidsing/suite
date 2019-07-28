package suite.telegram;

import static suite.util.Rethrow.ex;

import java.nio.file.Files;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import suite.cfg.Defaults;
import suite.streamlet.FunUtil2.FoldOp;
import suite.util.Thread_;

public class TelegramBot {

	public static void main(String[] args) {
		ApiContextInitializer.init();
		new TelegramBot().bot((userId, message) -> message);
	}

	public void bot(FoldOp<Integer, String> fun) {
		ex(() -> {
			return new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return "Kowloonbot";
				}

				public String getBotToken() {
					var path = Defaults.tmp("kowloonbot.token");
					return ex(() -> Files.readAllLines(path)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage()) {
						var message = update.getMessage();

						var sendMessage = new SendMessage();
						sendMessage.setChatId(message.getChat().getId().toString());
						sendMessage.setText(fun.apply(message.getFrom().getId(), message.getText()));

						ex(() -> sendApiMethod(sendMessage));
					}
				}
			});
		});

		while (true)
			Thread_.sleepQuietly(10000);
	}

}
