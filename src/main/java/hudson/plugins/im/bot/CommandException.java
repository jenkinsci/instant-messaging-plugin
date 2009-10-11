package hudson.plugins.im.bot;

/**
 * Is thrown when a bot command couldn't be executed.
 *
 * @author kutzi
 */
public class CommandException extends Exception {
    
    private static final long serialVersionUID = 1L;
    private String replyMessage ;

    public CommandException(String message) {
        super(message);
        this.replyMessage = message;
    }
    
    public CommandException(Throwable cause) {
        super(cause);
        this.replyMessage = null;
    }
    
    public CommandException(String message, Throwable cause) {
        super(message, cause);
        this.replyMessage = message;
    }
    
    public String getReplyMessage() {
        return replyMessage;
    }
}
