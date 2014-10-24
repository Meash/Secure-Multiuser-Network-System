package nz.ac.aut.hss.network;

import com.sun.istack.internal.Nullable;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/**
 * @author Martin Schrimpf
 * @created 18.07.2014
 */
public class MailSender extends AbstractMailClient {
	private final String address;
	private final String senderName;
	@Nullable
	private final MailAuthenticator authenticator;
	private Transport transport;

	/**
	 * @param authenticator if the authenticator is null, a connect without authentication is attempted
	 */
	public MailSender(final String host, final MailAuthenticator authenticator, final String address,
					  final String senderName) {
		super(host);
		this.authenticator = authenticator;
		if (address == null || address.isEmpty())
			throw new IllegalArgumentException("address must not be null or empty");
		this.address = address;
		if (senderName == null || senderName.isEmpty())
			this.senderName = address;
		else
			this.senderName = senderName;
	}

	@Override
	protected Session createSession(final Properties properties) {
		properties.setProperty("mail.smtp.auth", authenticator != null ? "true" : "false");
		properties.setProperty("mail.smtp.host", host);
		properties.setProperty("mail.smtp.ssl.trust", host);
		return super.createSession(properties);
	}

	@Override
	public void connect() throws ConnectionException {
		super.connect();
		try {
			if (transport == null) {
				transport = session.getTransport("smtp");
			}
			if (authenticator != null)
				transport.connect(host, authenticator.getUsername(), authenticator.getPassword());
			else
				transport.connect();
		} catch (MessagingException ex) {
			throw new ConnectionException(ex);
		}
	}

	public void disconnect() throws ConnectionException {
		try {
			if (transport != null && transport.isConnected())
				transport.close();
		} catch (MessagingException me) {
			System.err.println(me.getClass().getName() + " during disconnect: "
					+ me.getMessage());
		}
	}

	protected boolean isConnected() throws ConnectionException {
		return transport != null && transport.isConnected();
	}

	/**
	 * @throws javax.mail.MessagingException
	 * @throws IllegalArgumentException
	 * @throws java.io.UnsupportedEncodingException if our sender address has an unsupported encoding
	 * @see javax.mail.internet.InternetAddress#parse(String)
	 */
	public void sendMail(String recipientsAddresslist, String subject, String text)
			throws UnsupportedEncodingException, MessagingException, InterruptedException {
		sendMail(recipientsAddresslist, subject, createBodyPart(text));
	}

	/**
	 * @throws javax.mail.MessagingException
	 * @throws IllegalArgumentException
	 * @throws java.io.UnsupportedEncodingException if our sender address has an unsupported encoding
	 * @see javax.mail.internet.InternetAddress#parse(String)
	 */
	public void sendMail(String recipientsAddresslist, String subject, BodyPart... bodyParts)
			throws UnsupportedEncodingException, MessagingException, InterruptedException {
		// sender and recipient
		final Address[] recipients;
		try {
			recipients = InternetAddress.parse(recipientsAddresslist, false);
		} catch (AddressException e) {
			throw new IllegalArgumentException(
					"Could not parse recipients '" + recipientsAddresslist + "' to internet address");
		}
		sendMail(recipients, subject, bodyParts);
	}

	/**
	 * @throws javax.mail.MessagingException
	 * @throws IllegalArgumentException
	 * @throws java.io.UnsupportedEncodingException if our sender address has an unsupported encoding
	 * @see javax.mail.internet.InternetAddress#parse(String)
	 */
	public void sendMail(Address[] recipients, String subject, BodyPart... bodyParts)
			throws UnsupportedEncodingException, MessagingException, InterruptedException {
		// sender and recipient
		final Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(this.address, this.senderName));
		message.setRecipients(Message.RecipientType.TO, recipients);
		// content
		message.setSubject(subject);
		final Multipart multipart = new MimeMultipart();
		for (BodyPart bodyPart : bodyParts) {
			multipart.addBodyPart(bodyPart);
		}
		message.setContent(multipart);
		// send
		message.setSentDate(new Date());

		transport.sendMessage(message, message.getAllRecipients());
		message.saveChanges();
	}

	public static MimeBodyPart createBodyPart(String text) throws MessagingException {
		MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setText(text);
		return bodyPart;
	}

	public static MimeBodyPart createBodyPart(File file) throws MessagingException {
		return createBodyPart(file, file.getName());
	}

	public static MimeBodyPart createBodyPart(File file, String filename) throws MessagingException {
		DataSource source = new FileDataSource(file);
		MimeBodyPart bodyPart = new MimeBodyPart();
		bodyPart.setDataHandler(new DataHandler(source));
		bodyPart.setFileName(filename);
		return bodyPart;
	}

	@Override
	public String toString() {
		return "MailSender{" +
				"address='" + address + '\'' +
				", senderName='" + senderName + '\'' +
				", authenticator=" + authenticator +
				", transport=" + transport +
				'}';
	}
}
