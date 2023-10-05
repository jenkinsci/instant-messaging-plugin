package hudson.plugins.im;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.im.build_notify.DefaultBuildToChatNotifier;
import hudson.scm.ChangeLogSet;
import hudson.scm.NullSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
public class IMPublisherTest {

    private IMPublisher imPublisher;

    private AbstractBuild build;
    private final int buildNumber = 42;
    private AbstractBuild previousBuild;

    private AbstractBuild upstreamBuild;
    private AbstractBuild previousBuildUpstreamBuild;
    private AbstractBuild upstreamBuildBetweenPreviousAndCurrent;

    private AbstractProject project;
    private AbstractProject upstreamProject;
    private BuildListener listener;
    private RangeSet rangeset;

    @SuppressWarnings("unchecked")
    @Before
    // lot of ugly mocking going on here ...
    public void before() throws IOException {

        this.imPublisher = new IMTestPublisher();

        this.upstreamProject = mock(AbstractProject.class);
        this.project = mock(AbstractProject.class);
        this.rangeset = RangeSet.fromString(buildNumber + "-" + (buildNumber + 2), false);

        this.previousBuildUpstreamBuild = mock(AbstractBuild.class);
        this.upstreamBuildBetweenPreviousAndCurrent = mock(AbstractBuild.class);
        when(this.upstreamBuildBetweenPreviousAndCurrent.getDownstreamRelationship(this.project)).thenReturn(this.rangeset);

        this.upstreamBuild = mock(AbstractBuild.class);
        when(this.upstreamBuild.getDownstreamRelationship(this.project)).thenReturn(this.rangeset);

        createPreviousNextRelationShip(this.previousBuildUpstreamBuild, this.upstreamBuildBetweenPreviousAndCurrent,
                this.upstreamBuild);


        User user1 = mock(User.class);
        ChangeLogSet<TestEntry> changeLogSet1 = new TestChangeLogSet(this.previousBuildUpstreamBuild,
                new TestEntry(user1));
        User user2 = mock(User.class);
        when(user2.toString()).thenReturn("User2");
        ChangeLogSet<TestEntry> changeLogSet2 = new TestChangeLogSet(this.upstreamBuildBetweenPreviousAndCurrent,
                new TestEntry(user2));
        when(this.upstreamBuildBetweenPreviousAndCurrent.getChangeSet()).thenReturn(changeLogSet2);

        User user3 = mock(User.class);
        when(user3.toString()).thenReturn("User3");
        ChangeLogSet<TestEntry> changeLogSet3 = new TestChangeLogSet(this.upstreamBuild,
                new TestEntry(user3));
        when(this.upstreamBuild.getChangeSet()).thenReturn(changeLogSet3);


        this.previousBuild = mock(AbstractBuild.class);
        when(this.previousBuild.getResult()).thenReturn(Result.SUCCESS);
        when(this.previousBuild.getUpstreamRelationshipBuild(this.upstreamProject)).thenReturn(this.previousBuildUpstreamBuild);

        this.build = mock(AbstractBuild.class);
        when(this.build.getResult()).thenReturn(Result.FAILURE);
        when(this.build.getUpstreamRelationshipBuild(this.upstreamProject)).thenReturn(this.upstreamBuild);
        Map<AbstractProject, Integer> upstreamBuilds = Maps.newHashMap();
        upstreamBuilds.put(this.upstreamProject, -1); // number is unimportant, just needed to get the upstream projects
        when(this.build.getUpstreamBuilds()).thenReturn(upstreamBuilds);
        when(this.build.getProject()).thenReturn(this.project); // Seems required since https://github.com/jenkinsci/instant-messaging-plugin/pull/171 bump
        when(this.build.getParent()).thenReturn(this.project);  // => should pop out in AbstractBuild.getProject()
        when(this.build.getNumber()).thenReturn(this.buildNumber);

        createPreviousNextRelationShip2(this.previousBuild, this.build);

        this.listener = mock(BuildListener.class);
    }

    /**
     * Creates a previous/next relationship between the builds in the given order.
     */
    private static void createPreviousNextRelationShip(AbstractBuild... builds) {
        int max = builds.length - 1;

        AbstractBuild previousSuccessful = null;
        for (int i = 0; i < builds.length; i++) {
            if (builds[i].getResult() == Result.SUCCESS) {
                previousSuccessful = builds[i];
            }

            if (i < max) {
                when(builds[i].getNextBuild()).thenReturn(builds[i+1]);
                when(builds[i+1].getPreviousSuccessfulBuild()).thenReturn(previousSuccessful);
            }
        }

        for (int i = builds.length - 1; i >= 0; i--) {
            if (i >= 1) {
            }
        }
    }

    /**
     * Tests that all culprits from the previous builds upstream build (exclusive)
     * until the current builds upstream build (inclusive) are contained in the recipients
     * list.
     */
    @Test
    public void testIncludeUpstreamCulprits() throws Exception {
        /* Anticipating javax.mail.MessagingException and InterruptedException */
        Set<User> recipients = this.imPublisher.getNearestUpstreamCommitters(this.build, listener).keySet();

        assertEquals(recipients.toString(), 2, recipients.size());

        Iterable<String> userNamesIter = Iterables.transform(recipients, new Function<User, String>() {
            @Override
            public String apply(User from) {
                return from.toString();
            }
        });

        Set<String> userNames = Sets.newHashSet(userNamesIter);

        assertFalse(userNames.contains("User1"));
        assertTrue(userNames.contains("User2"));
        assertTrue(userNames.contains("User3"));
    }

    private static class IMTestPublisher extends IMPublisher {
        public IMTestPublisher() {
            super(Collections.<IMMessageTarget>emptyList(), NotificationStrategy.FAILURE_AND_FIXED.getDisplayName(),
                    true, true, true, true, true, new DefaultBuildToChatNotifier(), MatrixJobMultiplier.ALL);
        }

        @Override
        protected String getPluginName() {
            return null;
        }

        @Override
        protected IMConnection getIMConnection() throws IMException {
            return null;
        }

        @Override
        public BuildStepDescriptor<Publisher> getDescriptor() {
            return null;
        }

        @Override
        protected String getConfiguredIMId(User user) {
            return null;
        }
    }

    private static class TestEntry extends ChangeLogSet.Entry {

        private User author;

        public TestEntry(User author) {
            this.author = author;
        }

        @Override
        public String getMsg() {
            return null;
        }

        @Override
        public User getAuthor() {
            return this.author;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return null;
        }
    }

    private static class TestChangeLogSet extends ChangeLogSet<TestEntry> {

        private TestEntry entry;

        protected TestChangeLogSet(AbstractBuild<?, ?> build, TestEntry entry) {
            super(build, null);
            this.entry = entry;
        }

        @Override
        public Iterator<TestEntry> iterator() {
            return Iterators.forArray(this.entry);
        }

        @Override
        public boolean isEmptySet() {
            return false;
        }
    }
    private static void createPreviousNextRelationShip2(AbstractBuild... builds) {
        int max = builds.length - 1;
        AbstractBuild previousSuccessful = null;
        for (int i = 0; i < builds.length; i++) {
            if (builds[i].getResult() == Result.SUCCESS) {
                previousSuccessful = builds[i];
            }
            if (i < max) {
                when(builds[i + 1].getPreviousSuccessfulBuild()).thenReturn(previousSuccessful);
            }
        }
        for (int i = builds.length - 1; i >= 0; i--) {
            if (i >= 1) {
            }
        }
    }
}
