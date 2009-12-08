/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;

/**
 * Command pattern contract for Jabber bot commands.
 * 
 * @author Pascal Bleser
 * @see Bot
 */
public interface BotCommand {
	
	/**
	 * Execute a command.
	 * 
	 * @param chat the {@link IMChat} object, may be used to send reply messages
	 * @param message the original {@link IMMessage}
	 * @param sender the room nickname of the command sender // FIXME ckutz: replace with the FQ sender id!
	 * @param args arguments passed to the command, where <code>args[0]</code> is the command name itself
	 * @throws IMException
	 */
	public void executeCommand(final IMChat chat, final IMMessage message, String sender, final String[] args) throws IMException;
	
	/**
	 * Return the command usage text.
	 * @return the command usage text
	 */
	public String getHelp();
}
