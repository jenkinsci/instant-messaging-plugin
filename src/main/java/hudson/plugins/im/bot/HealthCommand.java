package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.HealthReport;
import hudson.plugins.im.tools.MessageHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Displays the health for one or several jobs.
 *
 * @author kutzi
 */
@Extension
public class HealthCommand extends AbstractMultipleJobCommand {
    @Override
    public Collection<String> getCommandNames() {
        return Arrays.asList("health","h");
    }

    @Override
    protected CharSequence getMessageForJob(AbstractProject<?, ?> project) {
        StringBuilder msg = new StringBuilder(32);
        msg.append(project.getFullDisplayName());
        if (project.isDisabled()) {
            msg.append("(disabled)");
        } else if (project.isBuilding()) {
            msg.append("(BUILDING: ").append(project.getLastBuild().getDurationString()).append(")");
        } else if (project.isInQueue()) {
            msg.append("(in queue)");
        }
        msg.append(": ");

        AbstractBuild<?, ?> lastBuild = project.getLastBuild();
        while ((lastBuild != null) && lastBuild.isBuilding()) {
            lastBuild = lastBuild.getPreviousBuild();
        }
        if (lastBuild != null) {
            msg.append("Health [");
            List<HealthReport> reports = project.getBuildHealthReports();
            if (reports.isEmpty() ) {
                reports = Collections.singletonList(project.getBuildHealth());
            }

            int i = 1;
            for (HealthReport health : reports) {
                msg.append(health.getDescription())
                    .append("(").append(health.getScore()).append("%)");
                if (i<reports.size()) {
                    msg.append(", ");
                }
                i++;
            }
            msg.append(": ").append(MessageHelper.getBuildURL(lastBuild));
        } else {
            msg.append("no finished build yet");
        }
        return msg;
    }

    @Override
    protected String getCommandShortName() {
        return "health";
    }
}
