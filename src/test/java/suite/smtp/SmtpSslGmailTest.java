package suite.smtp;

import org.junit.Test;

public class SmtpSslGmailTest {

	@Test
	public void test() {
		if (Boolean.FALSE)
			new SmtpSslGmail().send("stupidsing@gmail.com", "email mechanism is working", SmtpSslGmail.class.getName());
	}

}
