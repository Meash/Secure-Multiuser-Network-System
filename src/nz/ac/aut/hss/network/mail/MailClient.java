package nz.ac.aut.hss.network.mail;

import nz.ac.aut.hss.network.Application;

import javax.mail.*;
import javax.mail.search.SearchTerm;
import java.util.Properties;

/**
 * @author Martin Schrimpf
 * @created 24.10.2014
 */
public class MailClient extends AbstractMailClient {
	public static interface Defaults {
		public final int BUFFER_SIZE = 65536;
	}


	private final MailAuthenticator authenticator;
	private Store store;
	/**
	 * Buffer size to use for streaming from this server.
	 */
	private final int bufferSize;


	public MailClient(String host, int port, final MailAuthenticator authenticator) {
		this(host, port, authenticator, Defaults.BUFFER_SIZE);
	}


	public MailClient(String host, int port, final MailAuthenticator authenticator, int bufferSize) {
		super(host, port);
		this.authenticator = authenticator;

		if (bufferSize < 1) throw new IllegalArgumentException("bufferSize must be greater than zero");
		this.bufferSize = bufferSize;
	}

	/**
	 * Connects using the set values.
	 */
	public void connect() throws ConnectionException {
		super.connect();
		try {
			if (store == null) {
				store = session.getStore("imaps");
			}
			store.connect(host, authenticator.getUsername(), authenticator.getPassword());
		} catch (MessagingException e) {
			throw new ConnectionException("Could not connect", e);
		}
	}

	@Override
	protected Session createSession(final Properties props) {
		props.setProperty("mail.store.protocol", "imaps");
		props.put("mail.debug", Application.DEBUG ? "true" : "false");
		return super.createSession(props);
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

	public Message[] getMessages(Folder folder, SearchTerm searchTerm) throws MessagingException,
			InterruptedException {
		ensureFolderIsOpen(folder, Folder.READ_ONLY);
		if (searchTerm != null) {
			return folder.search(searchTerm);
		} else {
			return folder.getMessages();
		}
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

	@Override
	public String toString() {
		return "MailClient{" +
				"store=" + store +
				", bufferSize=" + bufferSize +
				'}';
	}
}
