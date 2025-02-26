package hudson.plugins.im;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.User;
import hudson.plugins.im.tools.ExceptionHelper;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Abstract implementation of a provider of {@link IMConnection}s.
 *
 * @author kutzi
 */
public abstract class IMConnectionProvider implements IMConnectionListener {

    private static final Logger LOGGER = Logger.getLogger(IMConnectionProvider.class.getName());

    private static final IMConnection NULL_CONNECTION = new DummyConnection();

    protected IMPublisherDescriptor descriptor;
    private IMConnection imConnection = NULL_CONNECTION;

    private Authentication authentication = null;

    private final ConnectorRunnable connector = new ConnectorRunnable();

    protected IMConnectionProvider() {
    }

    /**
     * Must be called once to initialize the provider.
     */
    protected void init() {
        Thread connectorThread = new Thread(this.connector, "IM-Reconnector-Thread");
        connectorThread.setDaemon(true);
        connectorThread.start();
        tryReconnect();
    }

    /**
     * Creates a new connection.
     *
     * @return the new connection. Never null.
     * @throws IMException if the connection couldn't be created for any reason.
     * @throws IMException
     */
    public abstract IMConnection createConnection() throws IMException;

    private synchronized boolean create() throws IMException {
        if (this.descriptor == null || !this.descriptor.isEnabled()) {
            // plugin is disabled
            this.imConnection = NULL_CONNECTION;
            return true;
        }

        try {
            this.imConnection = createConnection();
            this.imConnection.addConnectionListener(this);
            return true;
        } catch (IMException e) {
            LOGGER.log(Level.WARNING, "Could not create IM connection", e);
            this.imConnection = NULL_CONNECTION;
            tryReconnect();
            return false;
        }
    }

    /**
     * Return the current connection.
     * Returns an instance of {@link DummyConnection} if the plugin
     * is currently not connection to a IM network.
     *
     * @return the current connection. Never null.
     */
    public synchronized IMConnection currentConnection() {
        return this.imConnection;
    }

    /**
     * Releases (and thus closes) the current connection.
     */
    public synchronized void releaseConnection() {
        if (this.imConnection != null) {
            this.imConnection.removeConnectionListener(this);
            this.imConnection.close();
            this.imConnection = NULL_CONNECTION;
        }
    }

    protected synchronized IMPublisherDescriptor getDescriptor() {
        return this.descriptor;
    }

    public synchronized void setDescriptor(IMPublisherDescriptor desc) {
        this.descriptor = desc;

        if (desc != null && desc.isEnabled()) {
            tryReconnect();
        }
    }

    @Override
    public void connectionBroken(Exception e) {
        tryReconnect();
    }

    private void tryReconnect() {
        this.connector.semaphore.release();
    }

    // we need an additional level of indirection to the Authentication entity
    // to fix HUDSON-5978 and HUDSON-5233
    public synchronized AuthenticationHolder getAuthenticationHolder() {
        if (descriptor == null || descriptor.getHudsonUserName() == null) {
            return null;
        }

        return new AuthenticationHolder() {
            @Override
            public Authentication getAuthentication() {
                if (authentication != null) {
                    return authentication;
                }

                // New spotbugs UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR
                // just can't be quiesced, so duplicating the sanity-check here
                if (descriptor == null || descriptor.getHudsonUserName() == null) {
                    return null;
                }

                if (descriptor.getHudsonUserName().isBlank() && !(Jenkins.getInstance().isUseSecurity())) {
                    return null;
                }

                try {
                    User u = User.get(descriptor.getHudsonUserName());
                    return u.impersonate2();
                } catch (UsernameNotFoundException ue) {
                    if (descriptor.getHudsonUserName().isBlank()) {
                        throw new UsernameNotFoundException(
                            "No local Jenkins user name is configured for instant messaging to act as");
                    }
                    throw ue;
                }
            }
        };
    }

    private final class ConnectorRunnable implements Runnable {

        private final Semaphore semaphore = new Semaphore(0);

        private boolean firstConnect = true;

        public void run() {
            try {
                while (true) {
                    this.semaphore.acquire();

                    if (!firstConnect) {
                        // wait a little bit in case the XMPP server/network has just a 'hickup'
                        TimeUnit.SECONDS.sleep(30);
                        LOGGER.info("Trying to reconnect");
                    } else {
                        firstConnect = false;
                        LOGGER.info("Trying to connect");
                    }

                    boolean success = false;
                    int timeout = 1;
                    while (!success) {
                        synchronized (IMConnectionProvider.this) {
                            if (imConnection != null) {
                                try {
                                    releaseConnection();
                                } catch (Exception e) {
                                    LOGGER.warning(ExceptionHelper.dump(e));
                                }
                            }
                            try {
                                success = create();
                            } catch (IMException e) {
                                // ignore
                            }
                        }

                        // make sure to leave the synchronized block before sleeping!
                        if(!success) {
                            LOGGER.info("Reconnect failed. Next connection attempt in " + timeout + " minutes");
                            this.semaphore.drainPermits();

                            // wait up to timeout time OR until semaphore is released again (happens e.g. if global config was changed)
                            boolean acquired = this.semaphore.tryAcquire(timeout * 60L, TimeUnit.SECONDS);
                            if (acquired) {
                                this.semaphore.release();
                            } else {
                                LOGGER.warning("Could not acquire semaphore for " + (timeout * 60L) + "sec");
                            }
                            // exponentially increase timeout, but longer than 16 minutes
                            if (timeout < 15) {
                                timeout *= 2;
                            }
                        } else {
                            // remove any permits which came in in the mean time
                            this.semaphore.drainPermits();
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.info("Connect thread interrupted");
                // just bail out
            }
        }
    }
}
