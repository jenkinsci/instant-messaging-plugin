package hudson.plugins.im.bot;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.Extension;
import hudson.plugins.im.Sender;
import hudson.plugins.im.tools.MessageHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

/**
 * Give the bot a snack!
 * (this is really more to familiarize myself with working with Hudson/jabber
 * @author R. Tyler Ballance <tyler@slide.com>
 */
@Extension
public class SnackCommand extends AbstractTextSendingCommand {
    @Override
    public Collection<String> getCommandNames() {
        return Collections.singleton("botsnack");
    }

    private static final String HELP = " [<snack>] - om nom nom";

    private static final String[] THANKS = new String[] {
            "thanks a lot! om nom nom.",
            "you're so kind to me!",
            "yummy!",
            "great! yum yum." };

    private static final String[] THANKS_WITH_FOOD = new String[] {
            "I really like that %s",
            "how did you know that %s is my favorite food?",
            "I just love %s!",
            "I could eat %s all day long" };

    // Per modern guidelines, it is more efficient and safe to re-use
    // a single Random object than to keep allocating new ones
    private static final Random ran = new Random();

    @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE") // See also https://github.com/spotbugs/spotbugs/issues/1539
    @Override
    protected String getReply(Bot bot, Sender sender, String[] args) {
        String snack = null;
        if (args.length > 1) {
            snack = StringUtils.join(MessageHelper.copyOfRange(args, 1, args.length), " ");
        }

        StringBuilder msg = new StringBuilder(sender.getNickname()).append(": ");
        int index = ran.nextInt(THANKS.length);
        msg.append(THANKS[index]);

        if (snack != null) {
            msg.append(" ");
            index = ran.nextInt(THANKS_WITH_FOOD.length);
            msg.append(String.format(THANKS_WITH_FOOD[index], snack));
        }
        return msg.toString();
    }

    public String getHelp() {
        return HELP;
    }

}
