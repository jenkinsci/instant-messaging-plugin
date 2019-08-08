/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Queue;
import hudson.model.Queue.Item;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.Sender;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queue command for the instant messaging plugin bot.
 *
 * Generates a list of jobs waiting in the queue.
 *
 * @author Pascal Bleser
 */
@Extension
public class QueueCommand extends BotCommand {
    private static final String SYNTAX = " [~ regex pattern]";
    private static final String HELP = SYNTAX + " - show the state of the build queue, with optional '~ regex' filter on reported lines";

    @Override
    public Collection<String> getCommandNames() {
        return Arrays.asList("queue","q");
    }

    @Override
    public void executeCommand(Bot bot, IMChat chat, IMMessage message,
                               Sender sender, String[] args) throws IMException {
        Queue queue = Hudson.getInstance().getQueue();
        Item[] items = queue.getItems();
        StringBuffer msg = new StringBuffer();
        String filterRegex = null;
        Pattern filterPattern = null;

        // We are interested in args to the command, if any,
        // so starting from args[1] when (args.length >= 2)
        argsloop: // label for break to know which statement to abort
        for (int a = 1 ; args.length > a; a++) {
            switch (args[a]) {
                case "~": // the rest of line is the regex expression
                    if ( (args.length - a) < 1) {
                        msg.append("\n- WARNING: got ~ filtering argument for currentlyBuilding, but no filter value - so none was applied\n");
                        break argsloop;
                    }
                    for (int i = (a + 1); i < args.length; i++) {
                        if ( (a + 1) == i) {
                            // avoid appending to null
                            filterRegex = args[i];
                        } else {
                            // We can not really assume what the user
                            // entered if there were e.g. several
                            // whitespaces trimmed by line-parser.
                            // So if they meant modifiers (brackets,
                            // counts), they should spell them out.
                            // For clarity it would be preferable to
                            // pass a single token with \s+ where
                            // whitespaces are expected.
                            filterRegex += " " + args[i];
                        }
                    }
                    if ( filterRegex == null ) {
                        msg.append("\n- WARNING: got ~ filtering argument for queue, but failed to extract a filter value (maybe a bug in instant-messaging-plugin) - so none was applied\n");
                    } else {
                        msg.append("\n- NOTE: got ~ filtering argument for queue: applying regex filter to reported strings: " + filterRegex);
                        filterPattern = Pattern.compile(filterRegex);
                    }
                    break argsloop;
                default:
                    msg.append("\n- WARNING: got unsupported argument '" + args[a] + "' for queue, ignored; no filter was applied\n");
                    break;
            }
        }

        if (items.length > 0) {
            int countJobsInQueue = 0;
            int countJobsInPattern = 0;
            for (Item item : items) {
                StringBuffer msgLine = new StringBuffer();
                msgLine.append(item.task.getFullDisplayName())
                    .append(": ").append(item.getWhy());
                countJobsInQueue++;

                if (filterPattern != null) {
                    Matcher matcher = filterPattern.matcher(msgLine);
                    if (!matcher.find()) {
                        continue;
                    }
                    // We have a regex hit, report it
                    countJobsInPattern++;
                }

                msg.append("\n- ").append(msgLine);
            }
            if (items.length != countJobsInQueue) {
                msg.append("\n- WARNING: Internal queue array length was ")
                    .append(items.length)
                    .append(" while we counted ")
                    .append(countJobsInQueue)
                    .append(" items during listing! (maybe a bug in instant-messaging-plugin)");
            }
            if (countJobsInPattern == 0 && filterPattern != null) {
                msg.append("\n- None of the queued jobs matched the filter.");
            }
            if (filterPattern != null) {
                msg.insert(0, "Build queue (" + countJobsInQueue +
                    " items total, of which " + countJobsInPattern +
                    " items matched the filter):");
            } else {
                msg.insert(0, "Build queue (" + countJobsInQueue +
                    " items):");
            }
        } else {
            // Do not spam in the channel
            msg = null; // Drop and GC the now-useless argument-processing data
            msg = new StringBuffer();
            msg.append("Build queue is empty");
        }

        chat.sendMessage(msg.toString());
    }

    private String giveSyntax(String sender, String cmd) {
        return sender + ": syntax is: '" + cmd +  SYNTAX + "'";
    }

    @Override
    public String getHelp() {
        return HELP;
    }

}
