/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.plugins.im.AuthenticationHolder;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.IMMessageListener;
import hudson.plugins.im.Sender;
import hudson.plugins.im.bot.SetAliasCommand.AliasCommand;
import hudson.plugins.im.tools.ExceptionHelper;
import hudson.plugins.im.tools.MessageHelper;
import hudson.security.ACL;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.security.NotReallyRoleSensitiveCallable;

/**
 * Instant messaging bot.
 *
 * @author Pascal Bleser
 * @author kutzi
 */
public class Bot implements IMMessageListener {

	private static final Logger LOGGER = Logger.getLogger(Bot.class.getName());

    @Extension
	public static class HelpCommand extends BotCommand {
        @Override
        public Collection<String> getCommandNames() {
            return Collections.singleton("help");
        }

        public void executeCommand(Bot bot, IMChat groupChat, IMMessage message,
                                   Sender sender, String[] args) throws IMException {
			if (bot.helpCache == null) {
				final StringBuilder msg = new StringBuilder("Available commands:");
				for (final Entry<String, BotCommand> item : bot.cmdsAndAliases.entrySet()) {
					// skip myself
					if ((item.getValue() != this)
							&& (item.getValue().getHelp() != null)) {
						msg.append("\n");
						msg.append(item.getKey());
						msg.append(item.getValue().getHelp());
					}
				}
				bot.helpCache = msg.toString();
			}
			groupChat.sendMessage(bot.helpCache);
		}

		public String getHelp() {
			return null;
		}
	}

	private final SortedMap<String, BotCommand> cmdsAndAliases = new TreeMap<String, BotCommand>();

	private final IMChat chat;
	private final String nick;
	private final String imServer;
	private final String commandPrefix;
	private boolean commandsAccepted;
	private String helpCache = null;

	private final AuthenticationHolder authentication;

	public Bot(IMChat chat, String nick, String imServer,
			String commandPrefix, AuthenticationHolder authentication
			) {
		this.chat = chat;
		this.nick = nick;
		this.imServer = imServer;
		this.commandPrefix = commandPrefix;
		this.authentication = authentication;
        this.commandsAccepted = chat.isCommandsAccepted();

        for (BotCommand cmd : BotCommand.all()) {
            for (String name : cmd.getCommandNames())
                this.cmdsAndAliases.put(name,cmd);
        }

		chat.addMessageListener(this);
	}

    /**
     * Returns an identifier describing the Im account used to send the build command.
     *   E.g. the Jabber ID of the Bot.
     */
    public String getImId() {
        return this.nick + "@" + this.imServer;
    }

    public void onMessage(final IMMessage msg) {
        // is it a command for me ? (returns null if not, the payload if so)
        String payload = retrieveMessagePayLoad(msg.getBody());
        if (payload != null) {
            final Sender s = getSender(msg);

        	try {
            	if (!this.commandsAccepted) {
            	    this.chat.sendMessage(s.getNickname() + " you may not issue bot commands in this chat!");
            	    return;
            	} else if (!msg.isAuthorized()) {
    				this.chat.sendMessage(s.getNickname() + " you're not a buddy of me. I won't take any commands from you!");
    				return;
            	}
        	} catch (IMException e) {
                LOGGER.warning(ExceptionHelper.dump(e));
                return;
            }

            // split words
            final String[] args = MessageHelper.extractCommandLine(payload);
            if (args.length > 0) {
                // first word is the command name
                String cmd = args[0];

                try {
                	final BotCommand command = this.cmdsAndAliases.get(cmd);
                    if (command != null) {
                    	if (isAuthenticationNeeded()) {
                    		ACL.impersonate(this.authentication.getAuthentication(), new NotReallyRoleSensitiveCallable<Void, IMException>() {
								private static final long serialVersionUID = 1L;

								@Override
								public Void call() throws IMException {
									command.executeCommand(Bot.this, chat, msg, s, args);
									return null;
								}
							});
                    	} else {
                    		command.executeCommand(Bot.this, chat, msg, s, args);
                    	}
                    } else {
                        this.chat.sendMessage(s.getNickname() + " did you mean me? Unknown command '" + cmd
                                + "'\nUse '" + this.commandPrefix + " help' to get help!");
                    }
                } catch (Exception e) {
                    LOGGER.warning(ExceptionHelper.dump(e));
                }
            }
        }
	}

    private boolean isAuthenticationNeeded() {
    	return this.authentication != null && Jenkins.getInstance().isUseSecurity();
    }

	private Sender getSender(IMMessage msg) {
	    String sender = msg.getFrom();
	    String id = this.chat.getIMId(sender);

        final Sender s;
        if (id != null) {
            s = new Sender(this.chat.getNickName(sender), id);
        } else {
            s = new Sender(this.chat.getNickName(sender));
        }
        return s;
    }

    private static boolean isNickSeparator(final String candidate) {
		return ":".equals(candidate) || ",".equals(candidate);
	}

	private String retrieveMessagePayLoad(final String body) {
		if (body == null) {
			return null;
		}

		if (body.startsWith(this.commandPrefix)) {
			return body.substring(this.commandPrefix.length()).trim();
		}

		if (body.startsWith(this.nick)
				&& isNickSeparator(body.substring(this.nick.length(), this.nick
						.length() + 1))) {
			return body.substring(this.nick.length() + 1).trim();
		}

		return null;
	}

	/**
	 * Returns the command or alias associated with the given name
	 * or <code>null</code>.
	 */
	BotCommand getCommand(String name) {
		return this.cmdsAndAliases.get(name);
	}

	/**
	 * Registers a new alias.
	 *
	 * @return the alias previously registered under this name or <code>null</code>
	 * if no alias was registered by that name previously
	 * @throws IllegalArgumentException when trying to override a built-in command
	 */
	BotCommand addAlias(String name, BotCommand alias) {
		BotCommand old = this.cmdsAndAliases.get(name);
		if (old != null && ! (old instanceof AliasCommand)) {
			throw new IllegalArgumentException("Won't override built-in command: '" + name + "'!");
		}

		this.cmdsAndAliases.put(name, alias);
		this.helpCache = null;
		return old;
	}

	/**
	 * Removes an existing alias.
	 *
	 * @param name The name of the alias
	 * @return the removed alias or <code>null</code> if no alias by that name is registered
	 */
	AliasCommand removeAlias(String name) {
		BotCommand alias = this.cmdsAndAliases.get(name);
		if (alias instanceof AliasCommand) {
			this.cmdsAndAliases.remove(name);
			return (AliasCommand) alias;
		} else if (alias != null) {
			throw new IllegalArgumentException("Won't remove built-in command: '" + name + "'!");
		}
		return null;
	}

	/**
	 * Returns a map of all currently defined aliases.
	 * The map is sorted by the alias name.
	 */
	SortedMap<String, AliasCommand> getAliases() {
		SortedMap<String, AliasCommand> result = new TreeMap<String, AliasCommand>();
		for (Map.Entry<String, BotCommand> entry : this.cmdsAndAliases.entrySet()) {
			if (entry.getValue() instanceof AliasCommand) {
				result.put(entry.getKey(), (AliasCommand) entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Called on Jenkins shutdown.
	 */
	public void shutdown() {
		this.chat.removeMessageListener(this);

		if (this.chat.isMultiUserChat()) {
			try {
				chat.sendMessage("Oops, seems like Jenkins is going down now. See ya!");
			} catch (IMException e) {
				// ignore
			}
		}
	}
}
