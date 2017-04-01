package suite.smtp;

import java.util.Arrays;
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

import suite.Suite;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.node.Reference;
import suite.node.Str;

public class SmtpSslGmail {

	public void send(String to, String subject, String body) {
		Reference ref0 = new Reference();
		Reference ref1 = new Reference();
		String username, password;

		RuleSet rs = Suite.newRuleSet(Arrays.asList("secrets.sl"));
		Prover prover = new Prover(rs);

		if (prover.prove(Suite.substitute("gmail .0 .1", ref0, ref1))) {
			username = ((Str) ref0.finalNode()).value;
			password = ((Str) ref1.finalNode()).value;
		} else
			throw new RuntimeException();

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
	}

}
