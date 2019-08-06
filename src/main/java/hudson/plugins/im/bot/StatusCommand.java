/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.im.tools.MessageHelper;

import java.util.Arrays;
import java.util.Collection;


/**
 * Job/project status command for the jabber bot
 * @author Pascal Bleser
 */
@Extension
public class StatusCommand extends AbstractMultipleJobCommand {
    @Override
    public Collection<String> getCommandNames() {
        return Arrays.asList("status","s","jobs");
    }

    @Override
    protected CharSequence getMessageForJob(AbstractProject<?, ?> project) {
        StringBuilder msg = new StringBuilder(32);
        AbstractBuild<?, ?> lastBuild = project.getLastBuild();
        msg.append(project.getFullDisplayName());
        if (project.isDisabled()) {
            msg.append("(disabled) ");
        // a project which is building and additionally in queue should be reported as building
        } else if (project.isBuilding()) {
            msg.append("(BUILDING: ").append(lastBuild != null ? lastBuild.getDurationString() : "duration N/A").append(")");
        } else if (project.isInQueue()) {
            msg.append("(in queue) ");
        }
        msg.append(": ");

        while ((lastBuild != null) && lastBuild.isBuilding()) {
            lastBuild = lastBuild.getPreviousBuild();
        }
        if (lastBuild != null) {
            msg.append("last build: ").append(lastBuild.getNumber()).append(" (")
                .append(lastBuild.getTimestampString()).append(" ago): ").append(lastBuild.getResult()).append(": ")
                .append(MessageHelper.getBuildURL(lastBuild));
        } else {
            msg.append("no finished build yet");
        }
        return msg;
    }

    @Override
    protected String getCommandShortName() {
        return "status";
    }
}
