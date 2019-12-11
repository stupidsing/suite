package suite.telegram;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

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
		var subscribedChatIds = new HashSet<Long>();

		ex(() -> {
			var bot = new TelegramLongPollingBot() {
				public String getBotUsername() {
					return botUsername;
				}

				public String getBotToken() {
					return ex(() -> Files.readAllLines(tokenPath)).iterator().next();
				}

				public void onUpdateReceived(Update update) {
					if (update.hasMessage()) {
						var message = update.getMessage();
						var chatId = message.getChat().getId();
						var messageText = message.getText();

						if (messageText.startsWith("/"))
							if ("/subscribe".equalsIgnoreCase(messageText))
								subscribedChatIds.add(chatId);
							else if ("/unsubscribe".equalsIgnoreCase(messageText))
								subscribedChatIds.remove(chatId);
							else
								send(chatId, "unknown command");
						else
							send(chatId, fun.apply(message.getFrom().getId(), messageText));
					}
				}

				private void send(long chatId, String text) {
					var sendMessage = new SendMessage();
					sendMessage.setChatId(Long.toString(chatId));
					sendMessage.setText(text);
					ex(() -> sendApiMethod(sendMessage));
				}
			};

			new TelegramBotsApi().registerBot(bot);

			while (true) {
				Sleep.quietly(10000l);

				for (var chatId : subscribedChatIds)
					bot.send(chatId, "time is " + System.currentTimeMillis());
			}
		});
	}

}
