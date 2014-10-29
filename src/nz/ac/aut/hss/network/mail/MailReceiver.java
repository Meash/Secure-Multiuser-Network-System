package nz.ac.aut.hss.network.mail;

import javax.mail.*;
import javax.mail.search.DateTerm;
import javax.mail.search.ReceivedDateTerm;
import java.io.IOException;
import java.util.Date;

/**
 * @author Martin Schrimpf
 * @created 24.10.2014
 */
public class MailReceiver {
	private static final int UPDATE_TIMEOUT = 2000;
	private static final String INBOX_FOLDER = "INBOX";

	private final MailClient mailClient;
	private final Folder inboxFolder;
	private Date lastUpdateDate;

	public MailReceiver(final String host, int port, final MailAuthenticator authenticator)
			throws MessagingException, InterruptedException, ConnectionException {
		this.mailClient = new MailClient(host, port, authenticator);
		mailClient.connect();
		this.inboxFolder = mailClient.getAndValidateFolder(INBOX_FOLDER);
		lastUpdateDate = new Date();
	}

	/**
	 * Blocks until new messages have arrived.
	 * @return an array of all new messages in the inbox
	 */
	public Message[] waitForMessages() throws MessagingException, InterruptedException {
		Message[] messages;
		while (true) {
			messages = mailClient.getMessages(inboxFolder, new ReceivedDateTerm(DateTerm.GE, lastUpdateDate));
			if (messages.length != 0) {
				lastUpdateDate = new Date();
				break;
			} else {
				Thread.sleep(UPDATE_TIMEOUT);
			}
		}
		return messages;
	}

	public static String extractText(final Message msg) throws IOException, MessagingException {
		Object content = msg.getContent();
		if (!(content instanceof Multipart))
			throw new IllegalArgumentException("Content is not multipart");
		Multipart multipart = (Multipart) content;

		for (int i = 0; i < multipart.getCount(); i++) {
			BodyPart part = multipart.getBodyPart(i);
			if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
				return (String) part.getContent();
			}
		}

		throw new IllegalStateException("No text content found");
	}

	public void disconnect() {
		mailClient.disconnect();
	}
}
