package hudson.plugins.im;

/**
 * Listener for {@link IMConnection}s.
 *
 * @author kutzi
 */
public interface IMConnectionListener {

    /**
     * Is called whenever a connection broke - i.e. was closed because of an error, timeout, ...
     * Not called when a connection was called on our own request.
     *
     * @param e the exception which caused the close. May be null.
     */
	void connectionBroken(Exception e);
}
