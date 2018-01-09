package suite.smtp;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;

import suite.Constants;
import suite.primitive.Chars.CharsBuilder;

public class SmtpSslGmail {

	public void send(String to, String subject, String body) {
		Constants.bindSecrets("gmail .0 .1").map((username, enc) -> {
			String password = decode(System.getenv("USER").toCharArray(), enc);

			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "465");
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", SSLSocketFactory.class.getName());

			Session session = Session.getDefaultInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});

			try {
				String sender = username + "@gmail.com";

				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(sender));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to != null ? to : sender));
				message.setSubject(subject);
				message.setText(body);

				Transport.send(message);
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}

			return true;
		});
	}

	public static void main(String[] args) {
		char[] salt = "abc123".toCharArray();
		String in = "def456";
		String encoded = encode(salt, in);
		String decoded = decode(salt, encoded);
		System.out.println("encoded = " + encoded);
		System.out.println("decoded = " + decoded);
	}

	private static String encode(char[] salt, String in) {
		CharsBuilder cb0 = new CharsBuilder();
		char[] text0 = in.toCharArray();

		for (int i = 0; i < text0.length; i++) {
			int a = text0[i] + salt[i % salt.length];
			while (a < 32)
				a += 128 - 32;
			while (128 < a)
				a -= 128 - 32;
			cb0.append((char) a);
		}

		return cb0.toChars().toString();
	}

	private static String decode(char[] salt, String in) {
		CharsBuilder cb1 = new CharsBuilder();
		char[] text1 = in.toCharArray();

		for (int i = 0; i < text1.length; i++) {
			int a = text1[i] - salt[i % salt.length];
			while (a < 32)
				a += 128 - 32;
			while (128 < a)
				a -= 128 - 32;
			cb1.append((char) a);
		}

		return cb1.toChars().toString();
	}

}
