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
     */
    public String getNickName(String senderId);
    
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
