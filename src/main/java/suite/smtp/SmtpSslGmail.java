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
import suite.primitive.ChrChr_Int;
import suite.util.Fail;

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
				var sender = username + "@gmail.com";

				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(sender));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to != null ? to : sender));
				message.setSubject(subject);
				message.setText(body);

				Transport.send(message);
			} catch (MessagingException e) {
				Fail.t(e);
			}

			return true;
		});
	}

	public static void main(String[] args) {
		char[] salt = "abc123".toCharArray();
		var in = "def456";
		String encoded = encode(salt, in);
		String decoded = decode(salt, encoded);
		System.out.println("encoded = " + encoded);
		System.out.println("decoded = " + decoded);
	}

	private static String encode(char[] salt, String in) {
		return convert(salt, in, (a, b) -> a + b);
	}

	private static String decode(char[] salt, String in) {
		return convert(salt, in, (a, b) -> a - b);
	}

	private static String convert(char[] salt, String in0, ChrChr_Int f) {
		CharsBuilder cb = new CharsBuilder();
		char[] in1 = in0.toCharArray();

		for (int i = 0; i < in1.length; i++) {
			int a = f.apply(in1[i], salt[i % salt.length]);
			while (a < 32)
				a += 128 - 32;
			while (128 < a)
				a -= 128 - 32;
			cb.append((char) a);
		}

		return cb.toChars().toString();
	}

}
