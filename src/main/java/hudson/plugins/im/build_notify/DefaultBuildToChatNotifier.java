package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.ResultTrend;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;
import static hudson.plugins.im.tools.BuildHelper.*;
/**
 * {@link BuildToChatNotifier} that maintains the traditional behaviour of {@link IMPublisher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultBuildToChatNotifier extends SummaryOnlyBuildToChatNotifier {
    @DataBoundConstructor
    public DefaultBuildToChatNotifier() {
    }

    @Override
    public String buildStartMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder(super.buildStartMessage(publisher, build, listener));

        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
        if (previousBuild != null && !previousBuild.isBuilding()) {
            sb.append(" (previous build: ")
                .append(getResultTrend(previousBuild).getID());

            Result r = previousBuild.getResult();
            if (r == null || r.isWorseThan(Result.SUCCESS)) {
                AbstractBuild<?, ?> lastSuccessfulBuild = build.getPreviousSuccessfulBuild();
                if (lastSuccessfulBuild != null) {
                    sb.append(" -- last ").append(Result.SUCCESS).append(" ")
                        .append(lastSuccessfulBuild.getDisplayName())
                        .append(" ").append(lastSuccessfulBuild.getTimestampString()).append(" ago");
                }
            }
            sb.append(")");
        }

        return sb.toString();
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder(super.buildCompletionMessage(publisher, run,listener));

        List<ChangeLogSet<ChangeLogSet.Entry>> changelogSets = BuildHelper.getChangelogSets(run, listener);

        for (ChangeLogSet<ChangeLogSet.Entry> set : changelogSets) {
            if (!set.isEmptySet()) {
                boolean hasManyChangeSets = set.getItems().length > 1;
                for (Entry entry : set) {
                    sb.append("\n");
                    if (hasManyChangeSets) {
                        sb.append("* ");
                    }
                    sb.append(entry.getAuthor()).append(": ").append(entry.getMsg());
                }
            }
        }

        return sb.toString();
    }

    // set a high ordinal so that this comes in the top, as the default selection
    @Extension(ordinal=100)
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        @Override
        public String getDisplayName() {
            return "Summary + SCM changes";
        }
    }
}
