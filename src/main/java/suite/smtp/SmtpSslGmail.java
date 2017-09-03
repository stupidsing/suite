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

public class SmtpSslGmail {

	public void send(String to, String subject, String body) {
		Constants.bindSecrets("gmail .0 .1").map((username, password) -> {
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

}
