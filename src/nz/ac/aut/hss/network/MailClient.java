package nz.ac.aut.hss.network;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import java.util.Properties;

/**
 * @author Martin Schrimpf
 * @created 24.10.2014
 */
public abstract class MailClient {
	public static interface Defaults {
		public final int RESPONSE_TIMEOUT = 300000;
		public final int BUFFER_SIZE = 65536;
		public final int FETCH_SIZE = 819200;
	}


	protected final String host;
	protected Session session;

	private final MailAuthenticator authenticator;
	private Store store;
	/**
	 * Buffer size to use for streaming from this server.
	 */
	private final int bufferSize;


	public MailClient(String host, final MailAuthenticator authenticator) {
		this(host, authenticator, Defaults.BUFFER_SIZE);
	}


	public MailClient(String host, final MailAuthenticator authenticator, int bufferSize) {
		if (host == null)
			throw new IllegalArgumentException("host must not be null");
		this.host = host;
		this.authenticator = authenticator;

		if (bufferSize < 1) throw new IllegalArgumentException("bufferSize must be greater than zero");
		this.bufferSize = bufferSize;
	}

	/**
	 * Connects using the set values.
	 */
	public void connect() throws MessagingException {
		if (session == null)
			session = createSession();
		if (store == null) {
			store = session.getStore("imap");
		}
		store.connect(host, authenticator.getUsername(), authenticator.getPassword());
	}

	private Session createSession() {
		return createSession(System.getProperties());
	}

	protected Session createSession(final Properties properties) {
		// set timeout
		properties.setProperty("mail.imap.connectionpooltimeout", Defaults.RESPONSE_TIMEOUT + "");
		properties.setProperty("mail.imap.connectiontimeout", Defaults.RESPONSE_TIMEOUT + "");
		properties.setProperty("mail.imap.timeout", Defaults.RESPONSE_TIMEOUT + "");
		// workaround for partial FETCH bug in some IMAP servers - see http://stackoverflow.com/a/5292975/2225200
		properties.setProperty("mail.imaps.partialfetch", "false");
		// increase fetch size
		properties.setProperty("mail.imap.fetchsize", Defaults.FETCH_SIZE + "");
		return Session.getInstance(properties);
	}


	public void disconnect() {
		try {
			if (store != null && store.isConnected())
				store.close();
		} catch (MessagingException me) {
			System.err.println(me.getClass().getName() + " during disconnect: "
					+ me.getMessage());
		}
	}

	protected boolean isConnected() {
		return this.store != null && this.store.isConnected();
	}

	public void close() throws Exception {
		disconnect();
	}

	/**
	 * Checks if the folder is open and in the desired mode.
	 * Until this is not the case, a reopen is attempted.
	 * @param folder the folder to check
	 * @param mode   the desired folder mode
	 * @throws java.lang.InterruptedException
	 * @see Folder#READ_ONLY
	 * @see Folder#READ_WRITE
	 */
	public void ensureFolderIsOpen(final Folder folder, int mode) throws InterruptedException {
		if (folder == null)
			throw new IllegalArgumentException("folder must not be null");

		while (!folder.isOpen() || folder.getMode() < mode) {
			if (Thread.interrupted()) {
				throw new InterruptedException("Interrupted while reopening folder");
			}

			try {
				if (folder.isOpen()) { // open operation is not allowed on an open folder
					folder.close(false);
				}
				folder.open(mode);
			} catch (MessagingException e) {
				String msg = e.getClass().getName() + " while reopening folder: " + e.getMessage();
				System.out.println(msg);
			}
		}
	}

	public Message[] getMessages(String mailFolder) throws MessagingException,
			InterruptedException {
		Folder folder = getAndOpenFolder(mailFolder, Folder.READ_ONLY);
		return getMessages(folder);
	}

	public Message[] getMessages(Folder folder) throws MessagingException,
			InterruptedException {
		return getMessages(folder, null);
	}

	public Message[] getMessages(Folder folder, SearchTerm searchTerm) throws MessagingException,
			InterruptedException {
		ensureFolderIsOpen(folder, Folder.READ_ONLY);
		if (searchTerm != null) {
			return folder.search(searchTerm);
		} else {
			return folder.getMessages();
		}
	}

	/**
	 * Moves the message from the source-folder to the dest-folder.
	 * @param msg  the messages
	 * @param dest the dest-folder
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public void moveMessage(Message msg, String dest)
			throws MessagingException, InterruptedException {
		Message[] msgs = {msg};
		moveMessages(msgs, dest);
	}

	/**
	 * Moves all messages in the source-folder to the dest-folder
	 * @param msgs messages in the same folder
	 * @param dest the name of the destination folder
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public void moveMessages(Message[] msgs, String dest)
			throws MessagingException, InterruptedException {
		if (msgs.length == 0)
			return;

		Folder destFolder = store.getFolder(dest); // do not need to open according to api
		moveMessages(msgs, destFolder);
	}

	/**
	 * Moves the message from the source-folder to the dest-folder.
	 * @param msg  the messages
	 * @param dest the dest-folder
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public void moveMessage(Message msg, Folder dest)
			throws MessagingException, InterruptedException {
		Message[] msgs = {msg};
		moveMessages(msgs, dest);
	}

	/**
	 * Moves all messages in the source-folder to the dest-folder.
	 * This requires all {@code msgs} to be in the same folder.
	 * @param msgs       messages in the same folder
	 * @param destFolder the destination folder
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public void moveMessages(Message[] msgs, Folder destFolder)
			throws MessagingException, InterruptedException {
		if (msgs.length == 0)
			return;

		Folder srcFolder = msgs[0].getFolder();
		ensureFolderIsOpen(srcFolder, Folder.READ_WRITE);
		// destination folder does not have to be opened according to docs
		// move = copy + delete
		srcFolder.copyMessages(msgs, destFolder);
		srcFolder.setFlags(msgs, new Flags(Flags.Flag.DELETED), true);
		srcFolder.expunge();
	}

	public Message[] findMessages(Folder folder, String... messageIds) throws InterruptedException, MessagingException {
		MessageIDTerm[] messageIdTerms = new MessageIDTerm[messageIds.length];
		for (int i = 0; i < messageIdTerms.length; i++) {
			messageIdTerms[i] = new MessageIDTerm(messageIds[i]);
		}
		SearchTerm searchTerm = new OrTerm(messageIdTerms);

		ensureFolderIsOpen(folder, Folder.READ_ONLY);
		return folder.search(searchTerm);
	}

	public String getMessageId(final MimeMessage message) throws MessagingException, InterruptedException {
		ensureFolderIsOpen(message.getFolder(), Folder.READ_ONLY);
		return message.getMessageID();
	}

	public void deleteMessage(Message message) throws MessagingException, InterruptedException {
		deleteMessages(new Message[]{message});
	}

	/**
	 * Deletes all given messages in the folder.
	 * @param msgs the messages to delete
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public void deleteMessages(Message[] msgs)
			throws MessagingException, InterruptedException {
		if (msgs.length == 0)
			return;

		Folder folder = msgs[0].getFolder();
		ensureFolderIsOpen(folder, Folder.READ_WRITE);
		folder.setFlags(msgs, new Flags(Flags.Flag.DELETED), true);
		folder.expunge();
	}

	/**
	 * Finds a message by its id.
	 * @param messageId  the id of the message
	 * @param mailFolder the folder to search in
	 * @return the not-null message
	 * @throws MessagingException
	 * @throws InterruptedException
	 * @throws IllegalArgumentException if the message id does not resolve to a message
	 */
	public MimeMessage findMessageById(String messageId, String mailFolder)
			throws MessagingException, InterruptedException, IllegalArgumentException {
		Folder folder = getAndOpenFolder(mailFolder, Folder.READ_ONLY);
		Message[] msgs = folder.search(new MessageIDTerm(messageId));
		if (msgs.length > 1) {
			throw new IllegalStateException(
					"Message ID " + messageId + " is not unique (" + msgs.length + " messages found)");
		}
		if (msgs.length == 0 || !(msgs[0] instanceof MimeMessage)) {
			throw new IllegalArgumentException("Message ID " + messageId
					+ " does not resolve to a MimeMessage");
		}
		return (MimeMessage) msgs[0];
	}

	public Folder getDefaultFolder() throws MessagingException, InterruptedException {
		return store.getDefaultFolder();
	}

	public Folder getFolder(final String folderName) throws InterruptedException, MessagingException {
		return store.getFolder(folderName);
	}

	/**
	 * Retrieves the folder with the given name and validates its existence.
	 * Hence, the returned folder is guaranteed to exist
	 * @param folderName the name of the folder
	 * @return an existent folder
	 * @throws InterruptedException
	 * @throws MessagingException
	 * @throws FolderNotFoundException if the folder does not exist
	 */
	public Folder getAndValidateFolder(final String folderName)
			throws InterruptedException, MessagingException {
		Folder folder = getFolder(folderName);
		if (!folder.exists())
			throw new FolderNotFoundException(folder, "Folder '" + folderName + "' does not exist");
		return folder;
	}

	protected Folder getAndOpenFolder(final String folderName, final int mode)
			throws MessagingException, InterruptedException {
		Folder folder = getFolder(folderName);
		if (!folder.isOpen())
			folder.open(mode);
		return folder;
	}

	/**
	 * @param mailFolder the name of the folder to get the count from
	 * @return the amount of messages in the given folder
	 * @throws MessagingException
	 * @throws InterruptedException
	 */
	public int getMessageCount(String mailFolder) throws MessagingException,
			InterruptedException {
		Folder folder = getAndOpenFolder(mailFolder, Folder.READ_ONLY);
		return folder.getMessageCount();
	}

	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public String toString() {
		return "MailClient{" +
				"store=" + store +
				", bufferSize=" + bufferSize +
				'}';
	}
}
