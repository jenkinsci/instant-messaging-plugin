
package hudson.plugins.im;

/**
 * Represents a sender of a IM message 
 *
 * @author Christoph Kutzinski
 */
public class Sender {
    
    private final String nickname;
    private String id;

    public Sender(String nickname) {
        this.nickname = nickname;
    }
    
    public Sender(String nickname, String id) {
        this.nickname = nickname;
        this.id = id;
    }

    /**
     * The nickname of the sender.
     * This string is not necessarily unique - i.e. the nick in a chatroom. 
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the unique id of the sender.
     */
    public String getId() {
        return id;
    }
    
    public String toString() {
        return this.nickname;
    }
}
