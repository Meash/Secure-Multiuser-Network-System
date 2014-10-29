package nz.ac.aut.hss.network.mail;

import javax.mail.Session;
import java.util.Properties;

/**
 * @author Martin Schrimpf
 * @created 18.07.2014
 */
public abstract class AbstractMailClient  {
	protected final String host;
	protected final int port;
	protected Session session;

	protected AbstractMailClient(final String host, final int port) {
		this.port = port;
		if (host == null)
			throw new IllegalArgumentException("host must not be null");
		this.host = host;
	}

	public void connect() throws ConnectionException {
		if (session == null)
			session = createSession();
	}

	private Session createSession() {
		return createSession(System.getProperties());
	}

	protected Session createSession(Properties properties) {
		return Session.getInstance(properties);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final AbstractMailClient that = (AbstractMailClient) o;

		if (host != null ? !host.equals(that.host) : that.host != null) return false;
		if (session != null ? !session.equals(that.session) : that.session != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = host != null ? host.hashCode() : 0;
		result = 31 * result + (session != null ? session.hashCode() : 0);
		return result;
	}
}
