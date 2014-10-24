package nz.ac.aut.hss.network;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * @author Martin Schrimpf
 * @created 18.07.2014
 */
public class MailAuthenticator extends Authenticator {
	private final String username;
	private final String password;

	public MailAuthenticator(String username, String password) {
		if (username == null || username.isEmpty())
			throw new IllegalArgumentException("username must not be null or empty");
		this.username = username;
		if (password == null || password.isEmpty())
			throw new IllegalArgumentException("password must not be null or empty");
		this.password = password;
	}

	/**
	 * @return the <code>PasswordAuthentication</code>
	 * @see javax.mail.Authenticator#getPasswordAuthentication()
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(this.username, this.password);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final MailAuthenticator that = (MailAuthenticator) o;

		if (!password.equals(that.password)) return false;
		if (!username.equals(that.username)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = username.hashCode();
		result = 31 * result + (password.hashCode());
		return result;
	}
}
