package suite.telegram;

import org.junit.Test;

public class TelegramTest {

	@Test
	public void testBot() {
		new TelegramBot().bot((userId, message) -> message);
	}

}
