/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.Sender;

/**
 * Command pattern contract for Jabber bot commands.
 * 
 * @author Pascal Bleser
 * @author Christoph Kutzinski
 * @see Bot
 */
public interface BotCommand {
	
	/**
	 * Execute a command.
	 * 
	 * @param chat the {@link IMChat} object, may be used to send reply messages
	 * @param message the original {@link IMMessage}
	 * @param sender the command sender
	 * @param args arguments passed to the command, where <code>args[0]</code> is the command name itself
	 * @throws IMException
	 */
	public void executeCommand(IMChat chat, IMMessage message,
	        Sender sender, String[] args) throws IMException;
	
	/**
	 * Return the command usage text.
	 * @return the command usage text
	 */
	public String getHelp();
}
