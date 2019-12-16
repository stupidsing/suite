package suite.telegram;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.function.Predicate;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.BotSession;

import primal.Verbs.Sleep;
import primal.fp.Funs.Source;
import primal.fp.Funs2.FoldOp;
import primal.os.Log_;

public class TelegramBot {

	private String name = getClass().getSimpleName();

	private String botUsername;
	private Path tokenPath;
	private Predicate<String> login;
	private Source<String> alert;
	private FoldOp<Long, String> do_;

	private Deque<String> alerts = new ArrayDeque<>();

	public TelegramBot( //
			String botUsername, //
			Path tokenPath, //
			Predicate<String> login, //
			Source<String> alert, //
			FoldOp<Long, String> do_) {
		this.botUsername = botUsername;
		this.tokenPath = tokenPath;
		this.login = login;
		this.alert = alert;
		this.do_ = do_;
	}

	public boolean bot() {
		String interval = System.getenv("INTERVAL");
		long intervalMs = (interval != null ? Integer.valueOf(interval) : 15) * 1000l;

		var loggedInChatIds = Collections.synchronizedSet(new HashSet<Long>());
		var subscribedChatIds = Collections.synchronizedSet(new HashSet<Long>());

		return ex(() -> {
			TelegramBotsApi tba = new TelegramBotsApi();

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
						var messageText = message.getText();
						var messageTextLower = messageText.toLowerCase();
						var chatId = message.getChat().getId();

						Log_.info("[" + chatId + " => " + name + "] " + messageTextLower);

						if (messageTextLower.startsWith("/"))
							if (messageTextLower.startsWith("/login"))
								if (login.test(7 <= messageText.length() ? messageText.substring(7) : "")) {
									loggedInChatIds.add(chatId);
									send(chatId, "FINE");
								} else
									send(chatId, "FUCK OFF");
							else if (messageTextLower.startsWith("/logout")) {
								subscribedChatIds.remove(chatId);
								loggedInChatIds.remove(chatId);
								send(chatId, "CHILL OUT");
							} else if (loggedInChatIds.contains(chatId))
								if (messageTextLower.startsWith("/do"))
									send(chatId, do_.apply(chatId, messageText.substring(4)));
								else if (messageTextLower.startsWith("/status"))
									sendYouAreSubscribing(chatId);
								else if (messageTextLower.startsWith("/subscribe")) {
									subscribedChatIds.add(chatId);
									sendYouAreSubscribing(chatId);
								} else if (messageTextLower.startsWith("/unsubscribe")) {
									subscribedChatIds.remove(chatId);
									sendYouAreSubscribing(chatId);
								} else
									send(chatId, "UNKNOWN COMMAND");
							else
								send(chatId, "LOGIN FIRST");
						// send(chatId, fun.apply(message.getFrom().getId(), messageText));
					}
				}

				private void sendYouAreSubscribing(Long chatId) {
					var isSubscribing = subscribedChatIds.contains(chatId);
					send(chatId, "YOU ARE " + (isSubscribing ? "" : " NOT") + " SUBSCRIBING");
				}

				private void send(long chatId, String messageText) {
					Log_.info("[" + name + " => " + chatId + "]\n" + messageText);
					var sendMessage = new SendMessage();
					sendMessage.setChatId(Long.toString(chatId));
					sendMessage.setText(messageText);
					ex(() -> sendApiMethod(sendMessage));
				}
			};

			BotSession botSession = tba.registerBot(bot);

			while (botSession.isRunning()) {
				Sleep.quietly(intervalMs);
				String alert_ = alert.g();

				if (!alert_.isEmpty()) {
					for (var chatId : subscribedChatIds)
						bot.send(chatId, "LATEST:\n" + alert_);

					alerts.addLast(alert_);

					while (8 < alerts.size())
						alerts.removeFirst();
				}
			}

			return true;
		});
	}

}
