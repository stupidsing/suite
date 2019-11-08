package suite.telegram;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import primal.Verbs.Sleep;
import primal.fp.Funs2.FoldOp;

public class TelegramBot {

	private String botUsername;
	private Path tokenPath;

	public TelegramBot(String botUsername, Path tokenPath) {
		this.botUsername = botUsername;
		this.tokenPath = tokenPath;
	}

	public void bot(FoldOp<Integer, String> fun) {
		ex(() -> {
			return new TelegramBotsApi().registerBot(new TelegramLongPollingBot() {
				public String getBotUsername() {
					return botUsername;
				}

				public String getBotToken() {
					return ex(() -> Files.readAllLines(tokenPath)).iterator().next();
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
			Sleep.quietly(10000);
	}

}
