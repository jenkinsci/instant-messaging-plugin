package hudson.plugins.im;

/**
 * Listens to new messages.
 * 
 * @author kutzi
 */
public interface IMMessageListener {

    /**
     * Called whenever a new {@link IMMessage} is received.
     */
    void onMessage(IMMessage message);
}
