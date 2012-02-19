package hudson.plugins.im;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests the various notification options (only parent, only configurations, both) for Matrix jobs.
 * 
 * @author kutzi
 */
@SuppressWarnings("rawtypes")
public class MatrixNotificationTest {
    
    private IMPublisher publisher;
    private BuildListener listener;
    private AbstractBuild configurationBuild;
    private MatrixBuild parentBuild;

    @Before
    public void before() throws InterruptedException, IOException {
        this.publisher = mock(IMPublisher.class);
        when(publisher.prebuild(any(AbstractBuild.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.perform(any(AbstractBuild.class), any(Launcher.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.createAggregator(any(MatrixBuild.class), any(Launcher.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.getNotifyOnStart()).thenReturn(Boolean.TRUE);
        
        Mockito.doNothing().when(publisher).notifyChatsOnBuildStart(any(AbstractBuild.class), any(BuildListener.class));
        Mockito.doNothing().when(publisher).notifyOnBuildEnd(any(AbstractBuild.class), any(BuildListener.class));
        
        this.listener = mock(BuildListener.class);
        
        this.configurationBuild = mock(AbstractBuild.class);
        AbstractProject project = mock(MatrixConfiguration.class);
        when(configurationBuild.getParent()).thenReturn(project);
        
        this.parentBuild = mock(MatrixBuild.class);
    }

    @Test
    public void testOnlyParent() throws InterruptedException, IOException {
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
    public void testOnlyConfigurations() throws InterruptedException, IOException {
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
    public void testOnlyBoth() throws InterruptedException, IOException {
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
