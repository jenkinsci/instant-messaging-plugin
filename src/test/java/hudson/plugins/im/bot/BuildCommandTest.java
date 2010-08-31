package hudson.plugins.im.bot;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    @SuppressWarnings("unchecked")
    public void testDelay() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);
        
        AbstractProject project = mock(FreeStyleProject.class);
        when(jobProvider.getJobByName(Mockito.anyString())).thenReturn(project);
        
        Sender sender = new Sender("sender");
        
        cmd.getReply(bot, sender, new String[]{ "build", "project", "5s" });
        verify(project).scheduleBuild(eq(5), (Cause) Mockito.any());
        
        Mockito.reset(project);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "5" });
        verify(project).scheduleBuild(eq(5), (Cause) Mockito.any());
        
        Mockito.reset(project);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1m" });
        verify(project).scheduleBuild(eq(60), (Cause) Mockito.any());
        
        Mockito.reset(project);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1min" });
        verify(project).scheduleBuild(eq(60), (Cause) Mockito.any());
        
        Mockito.reset(project);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "2h" });
        verify(project).scheduleBuild(eq(7200), (Cause) Mockito.any());
        
        // TODO kutzi: this doesn't work, yet. Catch typo before 's'
        //Mockito.reset(project);
        //when(project.getQuietPeriod()).thenReturn(42);
        //cmd.getReply("sender", new String[]{ "build", "project", "1as" });
        //verify(project).scheduleBuild(eq(42), (Cause) Mockito.any());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testParameters() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);
        
        AbstractProject project = mock(FreeStyleProject.class);
        when(jobProvider.getJobByName(Mockito.anyString())).thenReturn(project);
        
        Sender sender = new Sender("sender");
        cmd.getReply(bot, sender, new String[]{ "build", "project", "key=value" });
        
        ArgumentCaptor<ParametersAction> captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).scheduleBuild(Mockito.anyInt(), (Cause) Mockito.any(),
                captor.capture());
        
        Assert.assertEquals(1, captor.getValue().getParameters().size());
        Assert.assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
        
        
        Mockito.reset(project);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "3s", "key=value", "key2=true" });
        captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).scheduleBuild(Mockito.anyInt(), (Cause) Mockito.any(),
                captor.capture());
        
        Assert.assertEquals(2, captor.getValue().getParameters().size());
        Assert.assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
        Assert.assertEquals(new BooleanParameterValue("key2", true),
                captor.getValue().getParameters().get(1));
    }
}
