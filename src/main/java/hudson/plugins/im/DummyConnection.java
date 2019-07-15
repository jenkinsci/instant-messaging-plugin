package hudson.plugins.im;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dummy implementation of {@link IMConnection}.
 *
 * @author kutzi
 */
class DummyConnection implements IMConnection {

	private static final Logger LOGGER = Logger.getLogger(DummyConnection.class.getName());

	@Override
	public void close() {
	}

	@Override
	public boolean connect() {
		return true;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public void send(IMMessageTarget target, String text) throws IMException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Send via dummy connection: '" + target + "' : '" + text + "'" );
		}
	}

	@Override
	public void setPresence(IMPresence presence, String statusMessage)
			throws IMException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Set presence via dummy connection: '" + presence + "' : '" + statusMessage + "'" );
		}
	}

	@Override
	public void addConnectionListener(IMConnectionListener listener) {
	}

	@Override
	public void removeConnectionListener(IMConnectionListener listener) {
	}
}
