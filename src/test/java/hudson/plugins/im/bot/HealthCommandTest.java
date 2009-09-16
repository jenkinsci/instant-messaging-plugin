package hudson.plugins.im.bot;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.plugins.im.bot.AbstractMultipleJobCommand;
import hudson.plugins.im.bot.HealthCommand;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test case for the 'health' command.
 * 
 * @author kutzi
 */
public class HealthCommandTest extends HudsonTestCase {

	private final Pattern percentagePattern = Pattern.compile("\\D(\\d+)[%]"); 
	
	
	public void testNoJobFound() {
		HealthCommand cmd = new HealthCommand();
		
		String sender = "tester";
		String[] args = {"health"};
		String reply = cmd.getReply(sender, args);
		
		assertEquals(sender + ": no job found", reply);
	}
	
	public void testHealth() throws Exception {
		FreeStyleProject p = createFreeStyleProject("fsProject");
		AbstractBuild<?, ?> build = p.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
		assertEquals(Result.SUCCESS, build.getResult());
		
		HealthCommand cmd = new HealthCommand();
		String sender = "tester";
		String[] args = {"health"};
		String reply = cmd.getReply(sender, args);
		System.out.println(reply);
		
		assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
		assertTrue(reply.contains("fsProject"));
		Matcher m = percentagePattern.matcher(reply);
		assertTrue(m.find());
		String match = m.group(1);
		assertEquals("100", match);
	}
	
	public void testFailure() throws Exception {
		FreeStyleProject p = createFreeStyleProject("fsProject");
		p.getBuildersList().add(new FailureBuilder());
		AbstractBuild<?, ?> build = p.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
		assertEquals(Result.FAILURE, build.getResult());
		
		HealthCommand cmd = new HealthCommand();
		String sender = "tester";
		String[] args = {"health"};
		{
			String reply = cmd.getReply(sender, args);
			System.out.println(reply);
		
			assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
			assertTrue(reply.contains("fsProject"));
			Matcher m = percentagePattern.matcher(reply);
			assertTrue(m.find());
			String match = m.group(1);
			assertEquals("0", match);
		}
		
		{
			p.getBuildersList().remove(FailureBuilder.class);
			build = p.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
			assertEquals(Result.SUCCESS, build.getResult());
			String reply = cmd.getReply(sender, args);
			System.out.println(reply);
		
			assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
			assertTrue(reply.contains("fsProject"));
			Matcher m = percentagePattern.matcher(reply);
			assertTrue(m.find());
			String match = m.group(1);
			int score = Integer.parseInt(match);
			// possibly because of rounding, Hudson gives 49 instead of 50 as score:
			assertTrue(score <= 51 && score >= 49);
		}
	}

	public void testGetByJobName() throws Exception {
		String projectName = "project name with spaces";
		createFreeStyleProject(projectName);
		
		HealthCommand cmd = new HealthCommand();
		String sender = "tester";

		{
			String[] projArgs = StringUtils.split(projectName);
			String[] cmdArg = {"health"};
			
			String[] args = new String[cmdArg.length + projArgs.length];
			
			for(int i = 0; i < cmdArg.length; i++) {
				args[i] = cmdArg[i];
			}
			for(int i = 0; i < projArgs.length; i++) {
				args[cmdArg.length + i] = projArgs[i];
			}
			
			String reply = cmd.getReply(sender, args);
			System.out.println(reply);
			assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
			assertTrue(reply.contains(projectName));
		}
		
		{
			String[] args = { "health", "project", "with", "foo" };
			String reply = cmd.getReply(sender, args);
			assertTrue(reply.contains(AbstractMultipleJobCommand.UNKNOWN_JOB_STR));
		}
	}
	
	public void testGetByView() throws Exception {
		String projectName = "project name";
		createFreeStyleProject(projectName);
		
		// Test with the all view
		String viewName = Messages.Hudson_ViewName();
		
		String[] args = { "health", "-v", viewName };
		HealthCommand cmd = new HealthCommand();
		String reply = cmd.getReply("sender", args);
		
		assertFalse(reply.contains(AbstractMultipleJobCommand.UNKNOWN_VIEW_STR));
		assertTrue(reply.contains(projectName));
		
	}
}
