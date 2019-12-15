package suite.telegram;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.function.Predicate;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import primal.Verbs.Sleep;
import primal.fp.Funs.Source;
import primal.fp.Funs2.FoldOp;

public class TelegramBot {

	private String botUsername;
	private Path tokenPath;
	private Predicate<String> verifyToken;
	private Source<String> alert;

	public TelegramBot(String botUsername, Path tokenPath, Predicate<String> verifyToken, Source<String> alert) {
		this.botUsername = botUsername;
		this.tokenPath = tokenPath;
		this.verifyToken = verifyToken;
		this.alert = alert;
	}

	public void bot(FoldOp<Long, String> fun) {
		var loggedInChatIds = new HashSet<Long>();
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
						var messageTextLower = messageText.toLowerCase();

						if (messageTextLower.startsWith("/"))
							if (messageTextLower.startsWith("/login")) {
								var token = messageText.substring(7);
								if (verifyToken.test(token)) {
									loggedInChatIds.add(chatId);
									send(chatId, "FINE");
								} else
									send(chatId, "FUCK OFF");
							} else if (messageTextLower.startsWith("/logout")) {
								subscribedChatIds.remove(chatId);
								loggedInChatIds.remove(chatId);
								send(chatId, "CHILL OUT");
							} else if (loggedInChatIds.contains(chatId))
								if (messageTextLower.startsWith("/do"))
									send(chatId, fun.apply(chatId, messageText.substring(4)));
								else if (messageTextLower.startsWith("/status"))
									sendYouAreSubscribed(chatId);
								else if (messageTextLower.startsWith("/subscribe")) {
									subscribedChatIds.add(chatId);
									sendYouAreSubscribed(chatId);
								} else if (messageTextLower.startsWith("/unsubscribe")) {
									subscribedChatIds.remove(chatId);
									sendYouAreSubscribed(chatId);
								} else
									send(chatId, "UNKNOWN COMMAND");
							else
								send(chatId, "LOGIN FIRST");
						// send(chatId, fun.apply(message.getFrom().getId(), messageText));
					}
				}

				private void sendYouAreSubscribed(Long chatId) {
					var isSubscribed = subscribedChatIds.contains(chatId);
					send(chatId, "YOU ARE " + (isSubscribed ? "" : " NOT") + " SUBSCRIBED");
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
					bot.send(chatId, alert.g());
			}
		});
	}

}
