package hudson.plugins.im.tools;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.Result;
import hudson.model.ResultTrend;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
     * Supports calling ResultTrend.getResultTrend() before there is a result
     * which would otherwise result in an NPE
     * @return NOT_BUILT if result isn't set (yet)
     */
    public static ResultTrend getResultTrend(Run<?, ?> run) {
        if (run.getResult() == null) {
            return ResultTrend.NOT_BUILT;
        }
        return ResultTrend.getResultTrend(run);
    }

    /**
     * This version uses ResultTrend
     *
     * Returns true if this build represents a 'fix'.
     * I.e. it is the first successful build after previous
     * 'failed' and/or 'unstable' builds.
     * Ignores 'aborted' and 'not built' builds.
     */
    public static boolean isFix(Run<?, ?> run) {
        return getResultTrend(run) == ResultTrend.FIXED;
    }

    /**
     * In the context of a pipeline job,
     * the step will potentially be used before the result has been set
     * In that case it can be considered a SUCCESS until proven otherwise.
     * @return what the name says
     */
    public static boolean isSuccessOrInProgress(Run<?, ?> run) {
        return run.getResult() == null || run.getResult() == Result.SUCCESS;
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
    public static String getProjectName(Run<?, ?> run) {
        return run.getParent().getFullDisplayName();
    }

    /**
     * Supports retrieving changelogsets from both AbstractBuild and from generic Run subclasses
     * @param run
     * @param listener
     * @return
     */
    public static List<ChangeLogSet<ChangeLogSet.Entry>> getChangelogSets(Run<?, ?> run, TaskListener listener) {
        if (run instanceof AbstractBuild) {
            return getChangelogSetsFromAbstractBuild((AbstractBuild) run);
        }
        return getChangelogSetsTheHardWay(run, listener);
    }


    public static Set<User> getCommitters(Run<?, ?> run, TaskListener listener) {
        List<ChangeLogSet<ChangeLogSet.Entry>> changelogSets = getChangelogSets(run, listener);
        final Set<User> users = new HashSet<>();
        for (ChangeLogSet set : changelogSets) {
            addChangeSetUsers(set, users);
        }
        return users;
    }

    public static List<ChangeLogSet<ChangeLogSet.Entry>> getChangelogSetsFromAbstractBuild(AbstractBuild build) {
        List<ChangeLogSet<ChangeLogSet.Entry>> result = new LinkedList();
        result.add(build.getChangeSet());
        return result;
    }


    public static List<ChangeLogSet<ChangeLogSet.Entry>> getChangelogSetsTheHardWay(final Run<?, ?> run, TaskListener listener) {
        List<ChangeLogSet<ChangeLogSet.Entry>> result = Collections.emptyList();
        // NOTE: code based on email-ext RecipientProviderUtilities.java, may not be needed down the line
        try {
            Method getChangeSets = run.getClass().getMethod("getChangeSets");
            if (List.class.isAssignableFrom(getChangeSets.getReturnType())) {
                result = new ArrayList<ChangeLogSet<ChangeLogSet.Entry>>((List<ChangeLogSet<ChangeLogSet.Entry>>) getChangeSets.invoke(run));
            }
        } catch (NoSuchMethodException  | InvocationTargetException | IllegalAccessException e) {
            listener.error("Exception getting changesets for %s: %s", run, e);
        }
        return result;
    }

    /**
     * Stolen from email-ext
     */
    private static void addChangeSetUsers(ChangeLogSet<?> changeLogSet, Set<User> users) {
        final Set<User> changeAuthors = new HashSet<User>();
        for (final ChangeLogSet.Entry change : changeLogSet) {
            final User changeAuthor = change.getAuthor();
            changeAuthors.add(changeAuthor);
        }
        users.addAll(changeAuthors);
    }

}
