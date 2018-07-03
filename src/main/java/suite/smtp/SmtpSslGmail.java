package suite.smtp;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;

import suite.Defaults;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrChr_Int;
import suite.util.Rethrow;

public class SmtpSslGmail {

	public void send(String to, String subject, String body) {
		Defaults.bindSecrets("gmail .0 .1").map((username, enc) -> {
			var password = decode(System.getenv("USER").toCharArray(), enc);

			var props = new Properties();
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

			var sender = username + "@gmail.com";

			return Rethrow.ex(() -> {
				var message = new MimeMessage(session);
				message.setFrom(new InternetAddress(sender));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to != null ? to : sender));
				message.setSubject(subject);
				message.setText(body);

				Transport.send(message);
				return message;
			});
		});
	}

	public static void main(String[] args) {
		var salt = "abc123".toCharArray();
		var in = "def456";
		var encoded = encode(salt, in);
		var decoded = decode(salt, encoded);
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
		var cb = new CharsBuilder();
		var in1 = in0.toCharArray();

		for (var i = 0; i < in1.length; i++) {
			var a = f.apply(in1[i], salt[i % salt.length]);
			while (a < 32)
				a += 128 - 32;
			while (128 < a)
				a -= 128 - 32;
			cb.append((char) a);
		}

		return cb.toChars().toString();
	}

}
