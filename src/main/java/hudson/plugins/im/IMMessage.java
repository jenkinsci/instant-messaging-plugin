package hudson.plugins.im;

/**
 * Represents a single message which is send in a IM protocol.
 *
 * @author kutzi
 */
public class IMMessage {

    private final String from;
    private final String to;
    private final String body;
	private boolean authorized;

    /**
     * Constructor.
     *
     * @param from The sender of the message
     * @param to The receiver of the message - this can e.g. be a 'user' or a 'chat room'
     * @param body The message body
     */
    public IMMessage(String from, String to, String body) {
        this(from, to, body, true);
    }

    public IMMessage(String from, String to, String body, boolean authorized) {
        this.from = from;
        this.to = to;
        this.body = body;
        this.authorized = authorized;
    }

    /**
     * Return the addressee of the message.
     * The result is in a protocol specific format.
     * May be null.
     */
    public String getTo() {
        return this.to;
    }

    /**
     * Return the sender of the message.
     * The result is in a protocol specific format.
     */
    public String getFrom() {
        return this.from;
    }

    /**
     * Returns the message body in a plain-text format.
     */
    public String getBody() {
        return this.body;
    }

    public boolean isAuthorized() {
    	return this.authorized ;
    }
}
