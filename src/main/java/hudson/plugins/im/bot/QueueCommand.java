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

/**
 * Queue command for the instant messaging plugin bot.
 *
 * Generates a list of jobs waiting in the queue.
 *
 * @author Pascal Bleser
 */
@Extension
public class QueueCommand extends BotCommand {

    private static final String HELP = " - show the state of the build queue";

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
        if (items.length > 0) {
            int count = 0;
            for (Item item : items) {
                msg.append("\n- ")
                .append(item.task.getFullDisplayName())
                .append(": ").append(item.getWhy());
                count++;
            }
            msg.insert(0, "Build queue (" + count + " items):");
            if (items.length != count) {
                msg.append("\n- WARNING: Internal queue array length was ")
                    .append(items.length)
                    .append(" while we counted ")
                    .append(countJobsInQueue)
                    .append(" items during listing! (maybe a bug in instant-messaging-plugin)");
            }
        } else {
            msg.append("build queue is empty");
        }

        chat.sendMessage(msg.toString());
    }

    @Override
    public String getHelp() {
        return HELP;
    }

}
