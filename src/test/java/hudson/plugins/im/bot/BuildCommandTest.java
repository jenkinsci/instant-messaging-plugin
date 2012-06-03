package hudson.plugins.im.bot;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.im.Sender;
import junit.framework.Assert;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class BuildCommandTest {

    @Test
    public void testDelay() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);
        
        AbstractProject<?, ?> project = mockProject(jobProvider);
        
        Sender sender = new Sender("sender");
        
        cmd.getReply(bot, sender, new String[]{ "build", "project", "5s" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(5), Mockito.any(Cause.class));
        
        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "5" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(5), Mockito.any(Cause.class));
        
        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1m" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(60), Mockito.any(Cause.class));
        
        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1min" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(60), Mockito.any(Cause.class));
        
        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "2h" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(7200), Mockito.any(Cause.class));
        
        // TODO kutzi: this doesn't work, yet. Catch typo before 's'
        //Mockito.reset(project);
        //when(project.getQuietPeriod()).thenReturn(42);
        //cmd.getReply("sender", new String[]{ "build", "project", "1as" });
        //verify(project).scheduleBuild(eq(42), (Cause) Mockito.any());
    }

    private AbstractProject<?, ?> mockProject(JobProvider jobProvider) {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(FreeStyleProject.class);
        when(jobProvider.getJobByName(Mockito.anyString())).thenReturn(project);
        when(project.hasPermission(Item.BUILD)).thenReturn(Boolean.TRUE);
        return project;
    }
    
    @Test
    public void testParameters() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);
        
        AbstractProject<?, ?> project = mockProject(jobProvider);
        
        Sender sender = new Sender("sender");
        cmd.getReply(bot, sender, new String[]{ "build", "project", "key=value" });
        
        ArgumentCaptor<ParametersAction> captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(Mockito.anyInt(), (Cause) Mockito.any(),
                captor.capture());
        
        Assert.assertEquals(1, captor.getValue().getParameters().size());
        Assert.assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
        
        
        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "3s", "key=value", "key2=true" });
        captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(Mockito.anyInt(), (Cause) Mockito.any(),
                captor.capture());
        
        Assert.assertEquals(2, captor.getValue().getParameters().size());
        Assert.assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
        Assert.assertEquals(new BooleanParameterValue("key2", true),
                captor.getValue().getParameters().get(1));
    }
}
