package hudson.plugins.im.build_notify;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
            AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException, InterruptedException {
        String msg = super.buildCompletionMessage(publisher, build, listener);
        return msg + getBuildParameters(build);
    }

    @Override
    public String culpritMessage(IMPublisher publisher,
            AbstractBuild<?, ?> build, BuildListener listener) {
        String msg = super.culpritMessage(publisher, build, listener);
        return msg + getBuildParameters(build);
    }

    @Override
    public String suspectMessage(IMPublisher publisher,
            AbstractBuild<?, ?> build, BuildListener listener,
            boolean firstFailure) {
        String msg = super.suspectMessage(publisher, build, listener, firstFailure);
        return msg + getBuildParameters(build);
    }

    @Override
    public String upstreamCommitterMessage(IMPublisher publisher,
            AbstractBuild<?, ?> build, BuildListener listener,
            AbstractBuild<?, ?> upstreamBuild) {
        String msg = super.upstreamCommitterMessage(publisher, build, listener, upstreamBuild);
        return msg + getBuildParameters(build);
    }

    private CharSequence getBuildParameters(AbstractBuild<?, ?> build) {

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
