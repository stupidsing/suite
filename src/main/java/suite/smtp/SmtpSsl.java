package suite.smtp;

import static primal.statics.Rethrow.ex;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;

import primal.primitive.ChrChr_Int;
import primal.primitive.fp.AsChr;
import suite.cfg.Defaults;

public class SmtpSsl {

	public void send(String to, String subject, String body) {
		Defaults.bindSecrets("mail .0 .1 .2").map((username, enc, sender) -> {
			var password = decode(Defaults.salt.toCharArray(), enc);

			var props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.host", "smtp.sendgrid.net");
			props.put("mail.smtp.port", "465");
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", SSLSocketFactory.class.getName());

			var session = Session.getDefaultInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});

			return ex(() -> {
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
		return AsChr.build(cb -> {
			var in1 = in0.toCharArray();

			for (var i = 0; i < in1.length; i++) {
				var a = f.apply(in1[i], salt[i % salt.length]);
				while (a < 32)
					a += 127 - 32;
				while (127 <= a)
					a -= 127 - 32;
				cb.append((char) a);
			}
		}).toString();
	}

}
