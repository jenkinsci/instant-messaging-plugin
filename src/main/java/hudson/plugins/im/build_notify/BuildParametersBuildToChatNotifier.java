package hudson.plugins.im.build_notify;

import java.io.IOException;
import java.util.List;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.plugins.im.IMPublisher;

/**
 * Extends {@link DefaultBuildToChatNotifier} and also prints
 * build parameters used in the build, if any.
 *
 * @author petehayes
 */
public class BuildParametersBuildToChatNotifier extends SummaryOnlyBuildToChatNotifier {

    @DataBoundConstructor
    public BuildParametersBuildToChatNotifier() {
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener)
            throws IOException, InterruptedException {
        String msg = super.buildCompletionMessage(publisher, run, listener);
        return msg + getBuildParameters(run);
    }

    @Override
    public String culpritMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener) {
        String msg = super.culpritMessage(publisher, run, listener);
        return msg + getBuildParameters(run);
    }

    @Override
    public String suspectMessage(IMPublisher publisher,
            Run<?, ?> run, TaskListener listener,
            boolean firstFailure) {
        String msg = super.suspectMessage(publisher, run, listener, firstFailure);
        return msg + getBuildParameters(run);
    }

    @Override
    public String upstreamCommitterMessage(IMPublisher publisher,
            Run<?, ?> build, TaskListener listener,
            Run<?, ?> upstreamBuild) {
        String msg = super.upstreamCommitterMessage(publisher, build, listener, upstreamBuild);
        return msg + getBuildParameters(build);
    }

    private CharSequence getBuildParameters(Run<?, ?> build) {

        ParametersAction parametersAction = build.getAction(ParametersAction.class);

        if (parametersAction == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        List<ParameterValue> parameters = parametersAction.getParameters();

        buf.append("\nParameters:");
        for (ParameterValue parameter : parameters) {
            buf.append("\n");
            buf.append(parameter.getShortDescription());
        }

        return buf;
    }

    @Extension
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {

        @Override
        public String getDisplayName() {
            return "Summary and build parameters";
        }
    }
}
