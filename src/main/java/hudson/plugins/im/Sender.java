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

    public String getNickname() {
        return nickname;
    }

    public String getId() {
        return id;
    }
}
