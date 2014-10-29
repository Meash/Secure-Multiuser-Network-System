package nz.ac.aut.hss.network.mail;

import com.sun.istack.internal.Nullable;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;

/**
 * @author Martin Schrimpf
 * @created 29.10.2014
 */
public class MailUtils {
	/**
	 * Returns the sender address of a {@link javax.mail.internet.MimeMessage} or null if no sender is set
	 * @param message the message
	 * @return the sender address
	 * @throws javax.mail.MessagingException
	 */
	@Nullable
	public static InternetAddress getSenderAddress(MimeMessage message)
			throws MessagingException {
		// get via sender
		InternetAddress senderAddr = (InternetAddress) message.getSender();
		if (senderAddr == null) {
			// get via from- and replyto-stack
			Address[] from = message.getFrom();
			Address[] replyTo = message.getReplyTo();
			Address[] addresses = concat(from, replyTo);
			if (addresses != null) {
				for (Address addr : addresses) {
					if (addr != null && addr instanceof InternetAddress) {
						senderAddr = (InternetAddress) addr;
						break;
					}
				}
			}
		}
		return senderAddr;
	}

	/**
	 * Concatenates two arrays placing the second array after the first one.
	 *
	 * @param first
	 *            the first array
	 * @param second
	 *            the second array
	 * @return the concatenated array with the length of first + second
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		if (first == null || first.length == 0) {
			return second;
		} else if (second == null || second.length == 0) {
			return first;
		}

		T[] concat = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, concat, first.length, second.length);
		return concat;
	}
}
