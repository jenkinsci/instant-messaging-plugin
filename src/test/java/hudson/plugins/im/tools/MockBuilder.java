package hudson.plugins.im.tools;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Mock {@link Builder} that always returns a configured {@link Result}
 */
public class MockBuilder extends Builder {
    
    private final Result result;

    public MockBuilder(Result result) {
        this.result = result;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Simulating an " + this.result + " build");
        build.setResult(this.result);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDisplayName() {
            return "Returns a pre-configured build result";
        }
    }
}
