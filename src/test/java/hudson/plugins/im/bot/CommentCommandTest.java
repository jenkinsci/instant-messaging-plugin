package hudson.plugins.im.bot;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.im.Sender;

import java.io.IOException;

import org.junit.Test;

public class CommentCommandTest {

    @Test
    public void testSetComment() throws IOException, CommandException {
        @SuppressWarnings({ "rawtypes" })
        AbstractProject project = mock(AbstractProject.class);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        when(project.getBuildByNumber(4711)).thenReturn(build);
        
        CommentCommand command = new CommentCommand();
        String result = command.getMessageForJob(project, new Sender("kutzi"),
                new String[] { "4711", "my comment"}).toString();
        assertEquals("Ok", result);
        
        verify(build).setDescription("my comment");
    }
    
    @Test(expected = CommandException.class)
    public void testMalformedBuildNumber() throws CommandException {
        AbstractProject<?, ?> project = mock(AbstractProject.class);
        
        CommentCommand command = new CommentCommand();
        command.getMessageForJob(project, new Sender("kutzi"),
                new String[] { "abc", "my comment"}).toString();
    }
    
    @Test(expected = CommandException.class)
    public void testUnknownBuildNumber() throws CommandException {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(AbstractProject.class);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        when(project.getBuildByNumber(4711)).thenReturn(build);
        
        CommentCommand command = new CommentCommand();
        command.getMessageForJob(project, new Sender("kutzi"),
                new String[] { "4712", "my comment"}).toString();
    }
}
