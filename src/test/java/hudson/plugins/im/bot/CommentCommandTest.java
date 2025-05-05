package hudson.plugins.im.bot;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import hudson.plugins.im.Sender;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentCommandTest {

    @Test
    void testSetComment() throws IOException, CommandException {
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

    @Test
    void testMalformedBuildNumber() {
        AbstractProject<?, ?> project = mock(AbstractProject.class);
        CommentCommand command = new CommentCommand();
        assertThrows(CommandException.class, () ->
            command.getMessageForJob(project, new Sender("kutzi"),
                    new String[]{"abc", "my comment"}).toString());
    }

    @Test
    void testUnknownBuildNumber() {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(AbstractProject.class);
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        CommentCommand command = new CommentCommand();
        assertThrows(CommandException.class, () ->
            command.getMessageForJob(project, new Sender("kutzi"),
                    new String[]{"4712", "my comment"}).toString());
    }
}
