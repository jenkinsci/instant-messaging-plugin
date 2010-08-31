package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.MessageHelper;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;

import static hudson.plugins.im.tools.BuildHelper.*;

/**
 * {@link BuildToChatNotifier} that maintains the traditional behaviour of {@link IMPublisher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultBuildToChatNotifier extends BuildToChatNotifier {
    // TODO: i18n
    @Override
    public String buildStartMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        final StringBuilder sb = new StringBuilder("Starting build ").append(build.getNumber())
            .append(" for job ").append(getProjectName(build));

        if (build.getPreviousBuild() != null) {
            sb.append(" (previous build: ")
                .append(BuildHelper.getResultDescription(build.getPreviousBuild()));

            if (build.getPreviousBuild().getResult().isWorseThan(Result.SUCCESS)) {
                AbstractBuild<?, ?> lastSuccessfulBuild = BuildHelper.getPreviousSuccessfulBuild(build);
                if (lastSuccessfulBuild != null) {
                    sb.append(" -- last ").append(Result.SUCCESS).append(" #")
                        .append(lastSuccessfulBuild.getNumber())
                        .append(" ").append(lastSuccessfulBuild.getTimestampString()).append(" ago");
                }
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        final StringBuilder sb;
        if (BuildHelper.isFix(build)) {
            sb = new StringBuilder(Messages.DefaultBuildToChatNotifier_BuildIsFixed());
        } else {
            sb = new StringBuilder();
        }
        sb.append(Messages.DefaultBuildToChatNotifier_Summary(
                getProjectName(build), build.getNumber(),
                BuildHelper.getResultDescription(build),
                build.getTimestampString(),
                MessageHelper.getBuildURL(build)));

        if (! build.getChangeSet().isEmptySet()) {
            boolean hasManyChangeSets = build.getChangeSet().getItems().length > 1;
            for (Entry entry : build.getChangeSet()) {
                sb.append("\n");
                if (hasManyChangeSets) {
                    sb.append("* ");
                }
                sb.append(entry.getAuthor()).append(": ").append(entry.getMsg());
            }
        }

        return sb.toString();
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        public String getDisplayName() {
            return "Summary + changes";
        }
    }
}
