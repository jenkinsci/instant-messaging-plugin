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
     * Translates the sender into a nickname which can be used to informally address the sender.
     * 
     * @param senderId the fully qualified IM id of the sender (e.g. for Jabber the user, the server domain and optional resource part)
     */
    public String getNickName(String senderId);
    
    /**
     * Translates the sender into a unique IM id.
     * 
     * Under certain circumstances the 'sender id' is not unique.
     * E.g. in a Jabber chatroom we will only get the 'nick' registered in the room
     * and not the real Jabber ID.
     * 
     * @param senderId
     * @return the 'real' ID or null if it couldn't be determined (e.g. because the room is anonymous) 
     */
    public String getIMId(String senderId);
    
    /**
     * Returns true if the chat is a multi-user chat (a Jabber conference room, an IRC chatroom)
     * as opposed to a single user chat (IRC private message exchange).
     */
    public boolean isMultiUserChat();

    /**
     * Returns if commands for Jenkins are accepted via this chat.
     * Otherwise this chat only acts as a notification target.
     * 
     * @since 1.21
     */
    public boolean isCommandsAccepted();
    
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
