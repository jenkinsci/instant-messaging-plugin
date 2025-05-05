package hudson.plugins.im;

import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the various notification options (only parent, only configurations, both) for Matrix jobs.
 *
 * @author kutzi
 */
@SuppressWarnings("rawtypes")
class MatrixNotificationTest {

    private IMPublisher publisher;
    private BuildListener listener;
    private AbstractBuild configurationBuild;
    private MatrixBuild parentBuild;

    @BeforeEach
    void setUp() throws InterruptedException, IOException {
        this.publisher = mock(IMPublisher.class);
        when(publisher.prebuild(any(AbstractBuild.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.perform(any(AbstractBuild.class), any(), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.createAggregator(any(MatrixBuild.class), any(), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.getNotifyOnStart()).thenReturn(Boolean.TRUE);

        Mockito.doNothing().when(publisher).notifyChatsOnBuildStart(any(AbstractBuild.class), any(BuildListener.class));
        Mockito.doNothing().when(publisher).notifyOnBuildEnd(any(AbstractBuild.class), any(BuildListener.class));

        this.listener = mock(BuildListener.class);

        this.configurationBuild = mock(AbstractBuild.class);
        AbstractProject project = mock(MatrixConfiguration.class);
        when(configurationBuild.getProject()).thenReturn(project); // Seems required since https://github.com/jenkinsci/instant-messaging-plugin/pull/171 bump
        when(configurationBuild.getParent()).thenReturn(project);  // => should pop out in AbstractBuild.getProject()

        this.parentBuild = mock(MatrixBuild.class);
    }

    @Test
    void testOnlyParent() throws InterruptedException, IOException {
        when(publisher.getMatrixNotifier()).thenReturn(MatrixJobMultiplier.ONLY_PARENT);

        publisher.prebuild(configurationBuild, listener);
        publisher.perform(configurationBuild, null, listener);
        verify(publisher, times(0)).notifyChatsOnBuildStart(any(AbstractBuild.class), any(BuildListener.class));
        verify(publisher, times(0)).notifyOnBuildEnd(any(AbstractBuild.class), any(BuildListener.class));

        MatrixAggregator aggregator = publisher.createAggregator(parentBuild, null, listener);
        aggregator.startBuild();
        aggregator.endBuild();
        verify(publisher).notifyChatsOnBuildStart(parentBuild, listener);
        verify(publisher).notifyOnBuildEnd(parentBuild, listener);
    }

    @Test
    void testOnlyConfigurations() throws InterruptedException, IOException {
        when(publisher.getMatrixNotifier()).thenReturn(MatrixJobMultiplier.ONLY_CONFIGURATIONS);

        MatrixAggregator aggregator = publisher.createAggregator(parentBuild, null, listener);
        aggregator.startBuild();
        aggregator.endBuild();
        verify(publisher, times(0)).notifyChatsOnBuildStart(parentBuild, listener);
        verify(publisher, times(0)).notifyOnBuildEnd(parentBuild, listener);

        publisher.prebuild(configurationBuild, listener);
        publisher.perform(configurationBuild, null, listener);
        verify(publisher).notifyChatsOnBuildStart(configurationBuild, listener);
        verify(publisher).notifyOnBuildEnd(configurationBuild, listener);
    }

    @Test
    void testOnlyBoth() throws InterruptedException, IOException {
        when(publisher.getMatrixNotifier()).thenReturn(MatrixJobMultiplier.ALL);

        MatrixAggregator aggregator = publisher.createAggregator(parentBuild, null, listener);
        aggregator.startBuild();
        aggregator.endBuild();
        verify(publisher).notifyChatsOnBuildStart(parentBuild, listener);
        verify(publisher).notifyOnBuildEnd(parentBuild, listener);

        publisher.prebuild(configurationBuild, listener);
        publisher.perform(configurationBuild, null, listener);
        verify(publisher).notifyChatsOnBuildStart(configurationBuild, listener);
        verify(publisher).notifyOnBuildEnd(configurationBuild, listener);
    }
}
