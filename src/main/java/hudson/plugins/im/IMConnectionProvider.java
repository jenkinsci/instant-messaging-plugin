package hudson.plugins.im;

import hudson.model.Hudson;
import hudson.plugins.im.tools.ExceptionHelper;
import hudson.util.TimeUnit2;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

/**
 * Abstract implementation of a provider of {@link IMConnection}s.
 * 
 * @author kutzi
 */
public abstract class IMConnectionProvider implements IMConnectionListener {
	
	private static final Logger LOGGER = Logger.getLogger(IMConnectionProvider.class.getName());
	
	private static final IMConnection NULL_CONNECTION = new DummyConnection();

	protected IMPublisherDescriptor descriptor;
	private IMConnection imConnection;
	
	private Authentication authentication = null;
	
	private final ConnectorRunnable connector = new ConnectorRunnable();
    
	protected IMConnectionProvider() {
		Thread connectorThread = new Thread(this.connector, "IM-Reconnector-Thread");
		connectorThread.setDaemon(true);
		connectorThread.start();
	}
	
	public abstract IMConnection createConnection() throws IMException;

    public synchronized IMConnection currentConnection() {
    	if (this.imConnection == null) {
    		try {
				this.imConnection = createConnection();
			} catch (IMException e) {
				tryReconnect();
				return NULL_CONNECTION;
			}
    		if (this.imConnection != null) {
    			this.imConnection.addConnectionListener(this);
    			return this.imConnection;
    		} else {
    			if (this.descriptor != null) {
    				tryReconnect();
    			}
    			return NULL_CONNECTION;
    		}
    	}
    	
        return this.imConnection;
    }

    /**
     * Releases (and thus closes) the current connection.
     */
    public synchronized void releaseConnection() {
        if (this.imConnection != null) {
        	this.imConnection.removeConnectionListener(this);
        	this.imConnection.close();
            this.imConnection = null;
        }
    }

	protected IMPublisherDescriptor getDescriptor() {
		return this.descriptor;
	}

	public void setDescriptor(IMPublisherDescriptor desc) {
		this.descriptor = desc;
		
		if (desc != null && desc.getHudsonUserName() != null) {
			try {
				Authentication tmp = new UsernamePasswordAuthenticationToken(desc.getHudsonUserName(),
						desc.getHudsonPassword());
				this.authentication = Hudson.getInstance().getSecurityRealm().getSecurityComponents().manager.authenticate(tmp);
			} catch (AuthenticationException e) {
			    LOGGER.warning(this.descriptor.getPluginDescription() 
			            + " couldn't authenticate against Hudson: " + e);
			}
		}
	}
	
    @Override
	public void connectionBroken(Exception e) {
		tryReconnect();
	}
    
    private void tryReconnect() {
    	this.connector.semaphore.release();
    }

	public Authentication getAuthentication() {
		return this.authentication;
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
								imConnection = createConnection();
								if (imConnection != null) {
									success = true;
								}
							} catch (IMException e) {
								// ignore
							}
						}
                        
                        // make sure to release the lock before sleeping!
                        if(!success) {
                            LOGGER.info("Reconnect failed. Next connection attempt in " + timeout + " minutes");
                            TimeUnit2.MINUTES.sleep(timeout);
                            // exponentially increase timeout
                            timeout = timeout * 2;
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
