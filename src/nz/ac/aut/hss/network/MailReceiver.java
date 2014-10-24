package nz.ac.aut.hss.network;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.ReceivedDateTerm;
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

	public MailReceiver(final MailClient mailClient) throws MessagingException, InterruptedException {
		this.mailClient = mailClient;
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
			messages = mailClient.getMessages(inboxFolder,
					new ReceivedDateTerm(1, lastUpdateDate));
			lastUpdateDate = new Date();
			if (messages.length != 0) {
				break;
			} else {
				Thread.sleep(UPDATE_TIMEOUT);
			}
		}
		return messages;
	}
}
