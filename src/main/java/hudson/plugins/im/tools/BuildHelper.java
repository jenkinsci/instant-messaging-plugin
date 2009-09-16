package hudson.plugins.im.tools;

import hudson.model.AbstractBuild;
import hudson.model.Result;

/**
 * Helper class to work with Hudson builds.
 *
 * @author kutzi
 */
public class BuildHelper {

    private BuildHelper() {
        // no instances
    }
    
    /**
     * Returns true if this build represents a 'fix'.
     * I.e. it is the first successful build after previous
     * 'failed' and/or 'unstable' builds.
     * Ignores 'aborted' and 'not built' builds.
     */
    public static boolean isFix(AbstractBuild<?, ?> build) {
        if (build.getResult() != Result.SUCCESS) {
            return false;
        }
        
        AbstractBuild<?, ?> previousBuild = getPreviousNonAbortedBuild(build);
        if (previousBuild != null) {
            return previousBuild.getResult().isWorseThan(Result.SUCCESS);
        }
        return false;
    }

    /**
     * Does what the name says.
     */
    public static boolean isFailureOrUnstable(AbstractBuild<?,?> build) {
    	return build.getResult() == Result.FAILURE
    		|| build.getResult() == Result.UNSTABLE;
    }

    /**
     * Returns the previous 'not aborted' build (i.e. ignores ABORTED and NOT_BUILT builds)
     * or null.
     */
    public static AbstractBuild<?, ?> getPreviousNonAbortedBuild(AbstractBuild<?, ?> build) {
        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
        while (previousBuild != null) {
            if (previousBuild.getResult() == Result.ABORTED || previousBuild.getResult() == Result.NOT_BUILT) {
                previousBuild = previousBuild.getPreviousBuild();
            } else {
                return previousBuild;
            }
        }
        return previousBuild;
    }
    
    /**
     * Returns a textual description of the result taking the previous build into
     * account.
     * E.g. reports 'fixes' and 'still failing/unstable' builds.
     */
    public static String getResultDescription(AbstractBuild<?, ?> build) {
        Result result = build.getResult();
        if (result == Result.ABORTED || result == Result.NOT_BUILT) {
            return result.toString();
        }
        
        if (isFix(build)) {
            return "FIXED";
        }
        
        AbstractBuild<?, ?> previousBuild = getPreviousNonAbortedBuild(build);
        if (result == Result.UNSTABLE) {
            if (previousBuild != null && previousBuild.getResult() == Result.UNSTABLE) {
                return "STILL UNSTABLE";
            }
        } else if (result == Result.FAILURE) {
            if (previousBuild != null && previousBuild.getResult() == Result.FAILURE) {
                return "STILL FAILING";
            }
        }
        
        return result.toString();
    }
}
