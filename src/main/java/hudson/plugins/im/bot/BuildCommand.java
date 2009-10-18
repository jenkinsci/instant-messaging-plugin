/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.model.Queue;
import hudson.plugins.im.IMCause;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build command for the instant messaging bot.
 * 
 * @author Pascal Bleser
 * @author kutzi
 */
public class BuildCommand implements BotCommand {
	
	private static final Pattern NUMERIC_EXTRACTION_REGEX = Pattern.compile("^(\\d+)");
	private static final String SYNTAX = " <job> [now|<delay[s|m|h]>]";
	private static final String HELP = SYNTAX + " - schedule a job build, with standard, custom or no quiet period";
	
	private final String imId;
	
	/**
	 * 
	 * @param imId An identifier describing the Im account used to send the build command.
	 *   E.g. the Jabber ID of the Bot.
	 */
	public BuildCommand(final String imId) {
		this.imId = imId;
	}

	private boolean scheduleBuild(AbstractProject<?, ?> project, int delaySeconds, String sender) {
		Cause cause = new IMCause("Started by " + this.imId + " on request of '" + sender + "'");
        return project.scheduleBuild(delaySeconds, cause);
	}

	@Override
	public void executeCommand(final IMChat chat, final IMMessage message, String sender,
			final String[] args) throws IMException {
		if (args.length >= 2) {
			String jobName = args[1];
			jobName = jobName.replaceAll("\"", "");
			
    		AbstractProject<?, ?> project = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
			if (project != null) {
				
    				if (project.isInQueue()) {
    					Queue.Item queueItem = project.getQueueItem();
						chat.sendMessage(sender + ": job " + jobName + " is already in the build queue (" + queueItem.getWhy() + ")");
        			} else if (project.isDisabled()) {
            					chat.sendMessage(sender + ": job " + jobName + " is disabled");
    				} else {
        					//project.scheduleBuild();
        					if ((args.length == 2) || (args.length == 3 && "now".equalsIgnoreCase(args[2]))) {
        						if (scheduleBuild(project, 1, sender)) {
                					chat.sendMessage(sender + ": job " + jobName + " build scheduled now");
  	     						} else {
	            					chat.sendMessage(sender + ": job " + jobName + " scheduling failed or already in build queue");
        						}
        					} else if (args.length >= 3) {
	            				final String delay = args[2].trim();
	            				int factor = 1;
	            				if (delay.endsWith("m") || delay.endsWith("min")) {
	            					factor = 60;
	            				} else if (delay.endsWith("h")) {
	            					factor = 3600;
	            				} else {
	            					char c = delay.charAt(delay.length() - 1);
	            					if (! (c == 's' || Character.isDigit(c))) {
	            						giveSyntax(chat, sender, args[0]);
	            						return;
	            					}
	            				}
	            				Matcher matcher = NUMERIC_EXTRACTION_REGEX.matcher(delay);
	            				if (matcher.find()) {
	            					int value = Integer.parseInt(matcher.group());
	                				if (scheduleBuild(project, value * factor, sender)) {
	    	                			chat.sendMessage(sender + ": job " + jobName + " build scheduled with a quiet period of " +
	    	                					(value * factor) + " seconds");
	                				} else {
	                					chat.sendMessage(sender + ": job " + jobName + " already scheduled in build queue");
	                				}
	            				}
        				
        					} else {
	            				if (scheduleBuild(project, project.getQuietPeriod(), sender)) {
		                			chat.sendMessage(sender + ": job " + jobName + " build scheduled (quiet period: " +
		                					project.getQuietPeriod() + " seconds)");
	            				} else {
	            					chat.sendMessage(sender + ": job " + jobName + " already scheduled in build queue");
	            				}
        					}
        				}
            		} else {
            			giveSyntax(chat, sender, args[0]);
            		}
		} else {
			chat.sendMessage(sender + ": Error, syntax is: '" + args[0] +  SYNTAX + "'");
		}
	}
	
	private void giveSyntax(IMChat chat, String sender, String cmd) throws IMException {
		chat.sendMessage(sender + ": syntax is: '" + cmd +  SYNTAX + "'");
	}

	public String getHelp() {
		return HELP;
	}

}
