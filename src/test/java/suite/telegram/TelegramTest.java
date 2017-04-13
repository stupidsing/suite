package suite.telegram;

import org.junit.Test;
import org.telegram.telegrambots.ApiContextInitializer;

public class TelegramTest {

	@Test
	public void testBot() {
		ApiContextInitializer.init();
		new TelegramBot().bot((userId, message) -> message);
	}

}
