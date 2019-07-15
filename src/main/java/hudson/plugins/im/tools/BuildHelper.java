package hudson.plugins.im.tools;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.ResultTrend;

/**
 * Helper class to work with Hudson builds.
 *
 * @author kutzi
 */
public class BuildHelper {

    /**
     * Extended result description of a build.
     *
     * @author kutzi
     *
     * @deprecated use {@link ResultTrend}!
     */
    @Deprecated
    public static enum ExtResult {
        FIXED, SUCCESS,
        /**
         * Marks a build which was previously a failure and is now 'only' unstable.
         */
        NOW_UNSTABLE("NOW UNSTABLE"),
        STILL_UNSTABLE("STILL UNSTABLE"), UNSTABLE,
        STILL_FAILING("STILL FAILING"), FAILURE,
        ABORTED, NOT_BUILT("NOT BUILT");

        private final String description;

        private ExtResult() {
            this.description = null;
        }

        private ExtResult(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.description != null ? this.description : super.toString();
        }
    }

    private BuildHelper() {
        // no instances
    }

    /**
     * Returns true if this build represents a 'fix'.
     * I.e. it is the first successful build after previous
     * 'failed' and/or 'unstable' builds.
     * Ignores 'aborted' and 'not built' builds.
     *
     * @deprecated use {@link ResultTrend#FIXED}!
     */
    @Deprecated
    public static boolean isFix(AbstractBuild<?, ?> build) {
        if (build.getResult() != Result.SUCCESS) {
            return false;
        }

        AbstractBuild<?, ?> previousBuild = getPreviousNonAbortedBuild(build);
        if (previousBuild != null) {
            Result previousResult = previousBuild.getResult();
			return previousResult != null && previousResult.isWorseThan(Result.SUCCESS);
        }
        return false;
    }

    /**
     * Does what the name says.
     *
     * @deprecated use {@link ResultTrend#FAILURE} || {@link ResultTrend#UNSTABLE}!
     */
    @Deprecated
    public static boolean isFailureOrUnstable(AbstractBuild<?,?> build) {
    	return build.getResult() == Result.FAILURE
    		|| build.getResult() == Result.UNSTABLE;
    }

    /**
     * @deprecated use {@link ResultTrend#STILL_FAILING} || {@link ResultTrend#STILL_UNSTABLE}!
     */
    @Deprecated
    public static boolean isStillFailureOrUnstable(AbstractBuild<?, ?> build) {
        ExtResult result = getExtendedResult(build);
        return result == ExtResult.STILL_FAILING || result == ExtResult.STILL_UNSTABLE;
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
     * Returns the previous successful build (i.e. build with result SUCCESS)
     * or null.
     *
     * @deprecated use {@link Run#getPreviousSuccessfulBuild()}
    */
    public static AbstractBuild<?, ?> getPreviousSuccessfulBuild(AbstractBuild<?, ?> build) {
        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
        while (previousBuild != null) {
            if (previousBuild.getResult() != Result.SUCCESS) {
                previousBuild = previousBuild.getPreviousBuild();
            } else {
                return previousBuild;
            }
        }
        return null;
    }

    /**
     * Returns a textual description of the result taking the previous build into
     * account.
     * E.g. reports 'fixes' and 'still failing/unstable' builds.
     *
     * @deprecated use ResultTrend.getResultTrend(build).getID()
     */
    @Deprecated
    public static String getResultDescription(AbstractBuild<?, ?> build) {
        ExtResult result = getExtendedResult(build);
        return result.toString();
    }

    /**
     * Returns the extended result description of a build.
     */
    public static ExtResult getExtendedResult(AbstractBuild<?, ?> build) {
        Result result = build.getResult();

        if (result == Result.ABORTED) {
            return ExtResult.ABORTED;
        } else if (result == Result.NOT_BUILT) {
            return ExtResult.NOT_BUILT;
        }

        if (result == Result.SUCCESS) {
            if (isFix(build)) {
                return ExtResult.FIXED;
            } else {
                return ExtResult.SUCCESS;
            }
        }

        AbstractBuild<?, ?> previousBuild = getPreviousNonAbortedBuild(build);
        if (result == Result.UNSTABLE) {
            if (previousBuild == null) {
                return ExtResult.UNSTABLE;
            }


            if (previousBuild.getResult() == Result.UNSTABLE) {
                return ExtResult.STILL_UNSTABLE;
            } else if (previousBuild.getResult() == Result.FAILURE) {
                return ExtResult.NOW_UNSTABLE;
            } else {
                return ExtResult.UNSTABLE;
            }
        } else if (result == Result.FAILURE) {
            if (previousBuild != null && previousBuild.getResult() == Result.FAILURE) {
                return ExtResult.STILL_FAILING;
            } else {
                return ExtResult.FAILURE;
            }
        }

        throw new IllegalArgumentException("Unknown result: '" + result + "' for build: " + build);
    }

    /**
     * Returns the name of the project the build belongs to in a human readable
     * format.
     */
    public static String getProjectName(AbstractBuild<?, ?> build) {
    	return build.getProject().getFullDisplayName();
    }
}
