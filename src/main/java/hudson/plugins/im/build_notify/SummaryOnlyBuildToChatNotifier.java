package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ResultTrend;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.MessageHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static hudson.plugins.im.tools.BuildHelper.*;

/**
 * {@link BuildToChatNotifier} that sends out a brief one line summary.
 *
 * @author Kohsuke Kawaguchi
 */
public class SummaryOnlyBuildToChatNotifier extends BuildToChatNotifier {
    @DataBoundConstructor
    public SummaryOnlyBuildToChatNotifier() {
    }

    @Override
    public String buildStartMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        return Messages.SummaryOnlyBuildToChatNotifier_StartMessage(build.getDisplayName(),getProjectName(build), publisher.getExtraMessage());
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        final StringBuilder sb;
        if (isFix(run)) {
            sb = new StringBuilder(Messages.SummaryOnlyBuildToChatNotifier_BuildIsFixed());
        } else {
            sb = new StringBuilder();
        }
        ResultTrend result = getResultTrend(run);
        sb.append(Messages.SummaryOnlyBuildToChatNotifier_Summary(
                getProjectName(run), run.getDisplayName(),
                result.getID(),
                run.getTimestampString(),
                MessageHelper.getBuildURL(run),
                publisher.getExtraMessage()));

        return sb.toString();
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        public String getDisplayName() {
            return "Just summary";
        }
    }
}
