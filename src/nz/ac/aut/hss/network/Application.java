package nz.ac.aut.hss.network;

import nz.ac.aut.hss.network.mail.*;
import sun.misc.BASE64Encoder;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Martin Schrimpf
 * @created 29.10.2014
 */
public class Application {

	public static final boolean DEBUG = false;

	public static void main(String[] args) throws Exception {
		final String keyStore = "keystorename", alias = "user", keyStorePassword = "pass", keyPassword = "pass";
		final Application app = new Application(keyStore, alias, keyStorePassword, keyPassword);
		System.out.println("Connecting...");
		app.init();
		System.out.println("OK");

		System.out.println("Starting mail receiver...");
		Thread t = app.startReceiverThread();
		System.out.println("OK");

		System.out.println("Sending mail");
		app.sendMailToSelf("Hi", "Some test mail " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
		System.out.println("OK");

		System.out.println("Waiting for mail-server");
		t.join();

		System.out.println("Disconnecting...");
		app.exit();
		System.out.println("OK");
	}

	private final String keyStore, alias, keyStorePassword, keyPassword;
	private final String imapHost = "imap.mail.yahoo.com", smtpHost = "smtp.mail.yahoo.com",
			ownName = "Barrack Obama",
			emailAddress = "sometrashmail123@yahoo.de",
			password = "Abcdef12";
	private final int imapPort = 993, smtpPort = 587;

	private MailSender mailSender;
	private MailReceiver mailReceiver;

	public Application(final String keyStore, final String alias, final String keyStorePassword,
					   final String keyPassword) {
		this.keyStore = keyStore;
		this.alias = alias;
		this.keyStorePassword = keyStorePassword;
		this.keyPassword = keyPassword;
	}

	private void init()
			throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
			IOException, ConnectionException, MessagingException, InterruptedException {
//		initKeys();
		initMail();
	}

	private void initKeys()
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException {
		KeyFetcher keyFetch = new KeyFetcher("keystoreA", "aliasA");
		//Test getting private key from key store works
		Key key = keyFetch.getPrivateKey(keyStore, keyStorePassword.toCharArray(), alias, keyPassword.toCharArray());
		BASE64Encoder encoder = new BASE64Encoder();
		System.out.println(encoder.encode(key.getEncoded()));
		//Test getting public key from file works
		PublicKey publicKey = (PublicKey) keyFetch.getPublicKey("certA.cer");
		System.out.println("\nPublic key of A from file system of B\n" + publicKey);
		//Test get public key of user B from trust store of user A
		PublicKey publicKeyFromTrust =
				(PublicKey) keyFetch.getPublicKeySecure("truststoreA", "password".toCharArray(), "aliasB");
		System.out.println("\nPublic Key of B from user A's truststore:\n " + publicKeyFromTrust);
	}

	private void initMail() throws ConnectionException, MessagingException, InterruptedException {
		final MailAuthenticator authenticator = new MailAuthenticator(emailAddress, password);

		// receiver
		mailReceiver = new MailReceiver(imapHost, imapPort, authenticator);

		// sender
		mailSender = new MailSender(smtpHost, smtpPort, authenticator, emailAddress, ownName);
		mailSender.connect();
	}

	private Thread startReceiverThread() {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Message[] msgs = mailReceiver.waitForMessages();
					for (Message msg : msgs) {
						System.out.println("Message received");
						System.out.printf("%10s: %s\n",
								"From",
								MailUtils.getSenderAddress((MimeMessage) msg).toString());
						System.out.printf("%10s: %s\n",
								"Subject",
								msg.getSubject());
						System.out.printf("%10s: %s\n",
								"Text",
								MailReceiver.extractText(msg));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		return t;
	}

	private void sendMailToSelf(final String subject, final String content)
			throws InterruptedException, UnsupportedEncodingException, MessagingException {
		mailSender.sendMail(emailAddress, subject, content);
	}

	private void exit() throws ConnectionException {
		mailSender.disconnect();
		mailReceiver.disconnect();
	}
}
