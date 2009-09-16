package hudson.plugins.im;

public class IMMessage {

    private final String from;
    private final String to;
    private final String body;

    public IMMessage(String from, String to, String body) {
        this.from = from;
        this.to = to;
        this.body = body;
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
}
