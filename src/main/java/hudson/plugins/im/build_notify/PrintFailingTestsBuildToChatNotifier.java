package hudson.plugins.im.build_notify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.MessageHelper;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Extends {@link DefaultBuildToChatNotifier} and also prints
 * failed tests if any.
 * Up to 5 failing tests are printed. Youngest failing tests are displayed first.
 *
 * @author kutzi
 */
public class PrintFailingTestsBuildToChatNotifier extends
        DefaultBuildToChatNotifier {

    @DataBoundConstructor
    public PrintFailingTestsBuildToChatNotifier() {
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener)
            throws IOException, InterruptedException {
        String msg = super.buildCompletionMessage(publisher, run, listener);
        return msg + getFailedTestsReport(run);
    }

    @Override
    public String culpritMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener) {
        String msg = super.culpritMessage(publisher, run, listener);
        return msg + getFailedTestsReport(run);
    }

    @Override
    public String suspectMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener,
            boolean firstFailure) {
        String msg = super.suspectMessage(publisher, run, listener, firstFailure);
        return msg + getFailedTestsReport(run);
    }

    @Override
    public String upstreamCommitterMessage(IMPublisher publisher,
            Run<?, ?> build, TaskListener listener,
            Run<?, ?> upstreamBuild) {
        String msg = super
                .upstreamCommitterMessage(publisher, build, listener, upstreamBuild);
        return msg + getFailedTestsReport(build);
    }

    private CharSequence getFailedTestsReport(Run<?, ?> build) {
        AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
        if (testResultAction == null || testResultAction.getFailCount() == 0) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        List<CaseResult> failedTests = testResultAction.getFailedTests();

        Collections.sort(failedTests, new Comparator<CaseResult>() {
            @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON",
                justification = "Maybe fix this inefficiency later, does not seem fatal")
            @Override
            public int compare(CaseResult o1, CaseResult o2) {
                if (o1.getAge() < o2.getAge()) {
                    return -1;
                } else if (o2.getAge() < o1.getAge()) {
                    return 1;
                }
                return o1.getFullName().compareTo(o2.getFullName());
            }
        });

        final int maxNumberOfTestsToPrint = 5;
        buf.append("\nFailed tests:\n");
        for (int i=0; i < Math.min(maxNumberOfTestsToPrint, failedTests.size()); i++) {
            CaseResult test = failedTests.get(i);
            buf.append(test.getFullName())
                .append(": ")
                .append(MessageHelper.getTestUrl(test))
                .append("\n");
        }

        int more = failedTests.size() - maxNumberOfTestsToPrint;
        if (more > 0) {
            buf.append("(").append(more).append(" more)");
        }

        return buf;
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        @Override
        public String getDisplayName() {
            return "Summary, SCM changes and failed tests";
        }
    }
}
