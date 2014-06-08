package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ResultTrend;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.MessageHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static hudson.plugins.im.tools.BuildHelper.*;
import hudson.tasks.test.AbstractTestResultAction;

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

    public String getFailureDiffString(AbstractTestResultAction<?> tests) {
        AbstractTestResultAction<?> prev = tests.getPreviousResult();
        if (prev == null) {
            return "";
        }

        return Functions.getDiffString(tests.getFailCount() - prev.getFailCount());
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        final StringBuilder sb;
        if (BuildHelper.isFix(build)) {
            sb = new StringBuilder(Messages.SummaryOnlyBuildToChatNotifier_BuildIsFixed());
        } else {
            sb = new StringBuilder();
        }

        AbstractTestResultAction<?> tests = build.getTestResultAction();
        sb.append(Messages.SummaryOnlyBuildToChatNotifier_Summary(
                getProjectName(build), build.getDisplayName(),
                ResultTrend.getResultTrend(build).getID(),
                build.getTimestampString(),
                MessageHelper.getBuildURL(build),
                tests.getTotalCount() - tests.getFailCount(),
                tests.getTotalCount(),
                tests.getFailCount(),
                this.getFailureDiffString(tests)));

        return sb.toString();
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        public String getDisplayName() {
            return "Just summary";
        }
    }
}
