package hudson.plugins.im.bot;

import hudson.maven.AbstractMavenProject;
import hudson.model.FreeStyleBuild;
import hudson.model.HealthReport;
import hudson.model.ItemGroup;
import hudson.plugins.im.Sender;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test case for the 'health' command.
 *
 * @author kutzi
 */
class HealthCommandTest {

    private final Pattern percentagePattern = Pattern.compile("\\D(\\d+)[%]");

    @Test
    void testNoJobFound() {
        JobProvider jobProvider = mock(JobProvider.class);
        HealthCommand cmd = new HealthCommand();
        cmd.setJobProvider(jobProvider);

        Sender sender = new Sender("tester");
        String[] args = {"health"};
        String reply = cmd.getReply(null, sender, args);

        assertEquals(sender + ": no job found", reply);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testHealth() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        when(build.getUrl()).thenReturn("job/foo/32/");

        HealthReport healthMock = mock(HealthReport.class);
        when(healthMock.getDescription()).thenReturn("Fine");
        when(healthMock.getScore()).thenReturn(100);

        AbstractMavenProject job = mock(AbstractMavenProject.class);
        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullDisplayName()).thenReturn("");
        when(job.getFullDisplayName()).thenReturn("fsProject");
        when(job.getLastBuild()).thenReturn(build);
        when(job.getBuildHealth()).thenReturn(healthMock);

        HealthCommand cmd = new HealthCommand();
        String reply = cmd.getMessageForJob(job).toString();

        assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
        assertTrue(reply.contains("fsProject"));
        Matcher m = percentagePattern.matcher(reply);
        assertTrue(m.find());
        String match = m.group(1);
        assertEquals("100", match);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testFailure() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        when(build.getUrl()).thenReturn("job/foo/32/");

        HealthReport healthMock = mock(HealthReport.class);
        when(healthMock.getDescription()).thenReturn("Cloudy");
        when(healthMock.getScore()).thenReturn(0);

        AbstractMavenProject job = mock(AbstractMavenProject.class);
        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullDisplayName()).thenReturn("");
        when(job.getFullDisplayName()).thenReturn("fsProject");
        when(job.getLastBuild()).thenReturn(build);
        when(job.getBuildHealth()).thenReturn(healthMock);

        HealthCommand cmd = new HealthCommand();
        String reply = cmd.getMessageForJob(job).toString();

        assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
        assertTrue(reply.contains("fsProject"));
        Matcher m = percentagePattern.matcher(reply);
        assertTrue(m.find());
        String match = m.group(1);
        assertEquals("0", match);
    }
}
