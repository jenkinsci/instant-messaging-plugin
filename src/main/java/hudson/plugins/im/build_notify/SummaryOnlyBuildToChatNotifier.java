package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
        return Messages.SummaryOnlyBuildToChatNotifier_StartMessage(build.getDisplayName(),getProjectName(build));
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        return Messages.SummaryOnlyBuildToChatNotifier_Summary(
               getProjectName(build), build.getDisplayName(),
               BuildHelper.getResultDescription(build),
               build.getTimestampString(),
               MessageHelper.getBuildURL(build));
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        public String getDisplayName() {
            return "Just summary";
        }
    }
}
