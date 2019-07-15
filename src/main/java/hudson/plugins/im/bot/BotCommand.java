/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Hudson;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.Sender;

import java.util.Collection;

/**
 * Command pattern contract for IM bot commands.
 *
 * <p>
 * To register custom bot commands, define a subtype and then put @{@link Extension} on your class.
 *
 * @author Pascal Bleser
 * @author Christoph Kutzinski
 * @author Kohsuke Kawaguchi
 * @see Bot
 */
public abstract class BotCommand implements ExtensionPoint {
    /**
     * Obtains the name of the command. Single commands can register multiple aliases,
     * so this method returns a collection.
     *
     * @return
     *      Can be empty but never null.
     */
    public abstract Collection<String> getCommandNames();

	/**
	 * Execute a command.
	 *
	 * @param bot
     *      The bot for which this command runs. Never null.
     * @param chat the {@link IMChat} object, may be used to send reply messages
     * @param message the original {@link IMMessage}
     * @param sender the command sender
     * @param args arguments passed to the command, where <code>args[0]</code> is the command name itself
     * @throws IMException if anything goes wrong while communicating with the remote IM server
	 */
	public abstract void executeCommand(Bot bot, IMChat chat, IMMessage message,
                                        Sender sender, String[] args) throws IMException;

	/**
	 * Return the command usage text.
	 * @return the command usage text
	 */
	public abstract String getHelp();

    /**
     * Returns all the registered {@link BotCommand}s.
     */
    public static ExtensionList<BotCommand> all() {
        return Hudson.getInstance().getExtensionList(BotCommand.class);
    }
}
