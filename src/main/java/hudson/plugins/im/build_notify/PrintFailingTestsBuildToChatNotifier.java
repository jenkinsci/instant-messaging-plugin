package hudson.plugins.im.build_notify;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.MessageHelper;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

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
			AbstractBuild<?, ?> build, BuildListener listener)
			throws IOException, InterruptedException {
		String msg = super.buildCompletionMessage(publisher, build, listener);
		return msg + getFailedTestsReport(build);
	}



	@Override
	public String culpritMessage(IMPublisher publisher,
			AbstractBuild<?, ?> build, BuildListener listener) {
		String msg = super.culpritMessage(publisher, build, listener);
		return msg + getFailedTestsReport(build);
	}

	@Override
	public String suspectMessage(IMPublisher publisher,
			AbstractBuild<?, ?> build, BuildListener listener,
			boolean firstFailure) {
		String msg = super.suspectMessage(publisher, build, listener, firstFailure);
		return msg + getFailedTestsReport(build);
	}

	@Override
	public String upstreamCommitterMessage(IMPublisher publisher,
			AbstractBuild<?, ?> build, BuildListener listener,
			AbstractBuild<?, ?> upstreamBuild) {
		String msg = super
				.upstreamCommitterMessage(publisher, build, listener, upstreamBuild);
		return msg + getFailedTestsReport(build);
	}

	private CharSequence getFailedTestsReport(AbstractBuild<?, ?> build) {

		AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
		if (testResultAction == null || testResultAction.getFailCount() == 0) {
			return "";
		}

		StringBuilder buf = new StringBuilder();
		List<CaseResult> failedTests = testResultAction.getFailedTests();
		Collections.sort(failedTests, new Comparator<CaseResult>() {
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
