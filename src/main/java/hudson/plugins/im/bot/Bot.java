/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.IMMessageListener;
import hudson.plugins.im.Sender;
import hudson.plugins.im.bot.SetAliasCommand.AliasCommand;
import hudson.plugins.im.tools.ExceptionHelper;
import hudson.plugins.im.tools.MessageHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * Instant messaging bot.
 * 
 * @author Pascal Bleser
 * @author kutzi
 */
public class Bot implements IMMessageListener {

	private static final Logger LOGGER = Logger.getLogger(Bot.class.getName());

	private class HelpCommand implements BotCommand {

		public void executeCommand(IMChat groupChat, IMMessage message,
				Sender sender, String[] args) throws IMException {
			if (helpCache == null) {
				final StringBuilder msg = new StringBuilder("Available commands:");
				for (final Entry<String, BotCommand> item : cmdsAndAliases.entrySet()) {
					// skip myself
					if ((item.getValue() != this)
							&& (item.getValue().getHelp() != null)) {
						msg.append("\n");
						msg.append(item.getKey());
						msg.append(item.getValue().getHelp());
					}
				}
				helpCache = msg.toString();
			}
			groupChat.sendMessage(helpCache);
		}

		public String getHelp() {
			return null;
		}

	};

	private static final Map<String, BotCommand> STATIC_COMMANDS_MAP;

	static {
		STATIC_COMMANDS_MAP = new HashMap<String, BotCommand>();
		STATIC_COMMANDS_MAP.put("status", new StatusCommand());
		STATIC_COMMANDS_MAP.put("s", new StatusCommand());
        STATIC_COMMANDS_MAP.put("health", new HealthCommand());
        STATIC_COMMANDS_MAP.put("h", new HealthCommand());
		STATIC_COMMANDS_MAP.put("jobs", new StatusCommand());
		STATIC_COMMANDS_MAP.put("queue", new QueueCommand());
		STATIC_COMMANDS_MAP.put("q", new QueueCommand());
		STATIC_COMMANDS_MAP.put("testresult", new TestResultCommand());
		STATIC_COMMANDS_MAP.put("abort", new AbortCommand());
		STATIC_COMMANDS_MAP.put("comment", new CommentCommand());
		STATIC_COMMANDS_MAP.put("botsnack", new SnackCommand());
	}
	
	private final SortedMap<String, BotCommand> cmdsAndAliases = new TreeMap<String, BotCommand>();

	private final IMChat chat;
	private final String nick;
	private final String imServer;
	private final String commandPrefix;
	private String helpCache = null;

	private final Authentication authentication;

	public Bot(IMChat chat, String nick, String imServer,
			String commandPrefix, Authentication authentication
			) {
		this.chat = chat;
		this.nick = nick;
		this.imServer = imServer;
		this.commandPrefix = commandPrefix;
		this.authentication = authentication;
		
		this.cmdsAndAliases.putAll(STATIC_COMMANDS_MAP);
		BuildCommand buildCommand  = new BuildCommand(this.nick + "@" + this.imServer);
		this.cmdsAndAliases.put("build", buildCommand);
		this.cmdsAndAliases.put("schedule", buildCommand);
		this.cmdsAndAliases.put("help", new HelpCommand());
		this.cmdsAndAliases.put("alias", new SetAliasCommand(this));
		
		
		chat.addMessageListener(this);
	}

	public void onMessage(IMMessage msg) {
        // is it a command for me ? (returns null if not, the payload if so)
        String payload = retrieveMessagePayLoad(msg.getBody());
        if (payload != null) {
        	String sender = msg.getFrom();
        	if (!msg.isAuthorized()) {
        		try {
					this.chat.sendMessage(sender + " you're not a buddy of me. I won't take any commands from you.");
				} catch (IMException e) {
					LOGGER.warning(ExceptionHelper.dump(e));
				}
				return;
        	}
        	
            // split words
            String[] args = MessageHelper.extractCommandLine(payload);
            if (args.length > 0) {
                // first word is the command name
                String cmd = args[0];
                
                String id = this.chat.getIMId(sender);
                
                final Sender s;
                if (id != null) {
                    s = new Sender(this.chat.getNickName(sender), id);
                } else {
                    s = new Sender(this.chat.getNickName(sender));
                }
                
                try {
                	BotCommand command = this.cmdsAndAliases.get(cmd);
                    if (command != null) {
                    	Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
                    	try {
                    	    if (this.authentication != null) {
                    	        SecurityContextHolder.getContext().setAuthentication(this.authentication);
                    	    }
	                    	command.executeCommand(this.chat, msg, s, args);
                    	} finally {
                    	    if (this.authentication != null) {
                    	        SecurityContextHolder.getContext().setAuthentication(oldAuthentication);
                    	    }
                	    }
                    } else {
                        this.chat.sendMessage(sender + " did you mean me? Unknown command '" + cmd
                                + "'\nUse " + this.commandPrefix + "help to get help!");
                    }
                } catch (IMException e) {
                    LOGGER.warning(ExceptionHelper.dump(e));
                }
            }
        }
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
	 * Called on Hudson shutdown.
	 */
	public void shutdown() {
		this.chat.removeMessageListener(this);
		
		if (this.chat.isMultiUserChat()) {
			try {
				chat.sendMessage("Oops, seems like Hudson is going down now. See ya!");
			} catch (IMException e) {
				// ignore
			}
		}
	}
}
