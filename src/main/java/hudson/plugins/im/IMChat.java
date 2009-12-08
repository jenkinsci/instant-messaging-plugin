package hudson.plugins.im;


/**
 * Abstraction of a chat.
 *
 * @author kutzi
 */
public interface IMChat {
    
    /**
     * Sends a message to the chat.
     *
     * @throws IMException If the message couldn't be delivered for any reason.
     */
    public void sendMessage(String message) throws IMException;
    
    /**
     * Translates the sender into a nickname which can be used to address the sender.
     * 
     * @param senderId the fully qualified IM id of the sender (e.g. for Jabber the user, the server domain and optional resource part)
     */
    public String getNickName(String senderId);
    
    /**
     * Returns true if the chat is a multi-user chat (a Jabber conference room, an IRC chatroom)
     * as opposed to a single user chat (IRC private message exchange).
     */
    public boolean isMultiUserChat();
    
    /**
     * Adds a new {@link IMMessageListener} to this chat.
     * 
     * Note that certain protocols/APIs might not support this method
     * - in that case this method should do nothing.
     */
    public void addMessageListener(IMMessageListener listener);
    
    /**
     * Removes a {@link IMMessageListener} from this chat.
     * 
     * Note that certain protocols/APIs might not support this method
     * - in that case this method should do nothing.
     * 
     * Note also that Smack Jabber API (2.x) does not support this even
     * if one has added a message listener previously.
     */
    public void removeMessageListener(IMMessageListener listener);
}
