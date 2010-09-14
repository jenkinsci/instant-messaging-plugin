/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.plugins.im.IMCause;
import hudson.plugins.im.Sender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build command for the instant messaging bot.
 * 
 * @author Pascal Bleser
 * @author kutzi
 */
@Extension
public class BuildCommand extends AbstractTextSendingCommand {
	
	private static final Pattern NUMERIC_EXTRACTION_REGEX = Pattern.compile("^(\\d+)");
	private static final String SYNTAX = " <job> [now|<delay>[s|m|h]] [<parameterkey>=<value>]*";
	private static final String HELP = SYNTAX + " - schedule a job build, with standard, custom or no quiet period";
	
    @Override
    public Collection<String> getCommandNames() {
        return Arrays.asList("build","schedule");
    }

    private boolean scheduleBuild(Bot bot, AbstractProject<?, ?> project, int delaySeconds, Sender sender, List<ParameterValue> parameters) {
	    
	    String senderId = sender.getId();
	    if (senderId == null) {
	        senderId = sender.getNickname();
	    }
	    
		Cause cause = new IMCause("Started by " + bot.getImId() + " on request of '" + senderId + "'");
		if (parameters.isEmpty()) {
		    return project.scheduleBuild(delaySeconds, cause);
		} else {
		    return project.scheduleBuild(delaySeconds, cause, new ParametersAction(parameters));
		}
	}

	@Override
	public String getReply(Bot bot, Sender sender, String args[]) {
		if (args.length >= 2) {
			String jobName = args[1];
			jobName = jobName.replaceAll("\"", "");
    		AbstractProject<?, ?> project = getJobProvider().getJobByName(jobName);
			if (project != null) {

			    String msg = "";
				if (project.isInQueue()) {
					Queue.Item queueItem = project.getQueueItem();
					return sender.getNickname() + ": job " + jobName + " is already in the build queue (" + queueItem.getWhy() + ")";
    			} else if (project.isDisabled()) {
					return sender.getNickname() + ": job " + jobName + " is disabled";
				} else {
				    
				    int delay = project.getQuietPeriod();
				    List<ParameterValue> parameters = new ArrayList<ParameterValue>();
				    
				    if (args.length >= 3) {
				        
				        int parametersStartIndex = 2;
				        if (!args[2].contains("=")) { // otherwise looks like a parameter
				            
				            parametersStartIndex = 3;
				            
				            String delayStr = args[2].trim();
				            if ("now".equalsIgnoreCase(delayStr)) {
				                delay = 0;
				            } else {
    				            int multiplicator = 1;
    				            if (delayStr.endsWith("m") || delayStr.endsWith("min")) {
    				                multiplicator = 60;
                                } else if (delayStr.endsWith("h")) {
                                    multiplicator = 3600;
                                } else {
                                    char c = delayStr.charAt(delayStr.length() - 1);
                                    if (! (c == 's' || Character.isDigit(c))) {
                                        return giveSyntax(sender.getNickname(), args[0]);
                                    }
                                }
    				            
    				            Matcher matcher = NUMERIC_EXTRACTION_REGEX.matcher(delayStr);
                                if (matcher.find()) {
                                    int value = Integer.parseInt(matcher.group(1));
                                    delay = multiplicator * value;
                                } else {
                                    return giveSyntax(sender.getNickname(), args[0]);
                                }
				            }
				        }
				        
				        // parse possible parameters:
				        for (int i=parametersStartIndex; i < args.length; i++) {
				            String[] split = args[i].split("=");
				            // TODO: guess ParameterValue type from ParametersDefintion of job?
				            if (split.length == 2) {
				                String value = split[1];
				                if ("true".equals(value) || "false".equals(value)) {
				                    parameters.add(new BooleanParameterValue(split[0], Boolean.parseBoolean(split[1])));
				                } else {
				                    parameters.add(new StringParameterValue(split[0], split[1]));
				                }
				            } else {
				                msg += "Unparseable parameter: " + args[i];
				            }
				        }
				    }
				    
				    
				    if (!parameters.isEmpty() && !project.isParameterized()) {
				        msg += "Ignoring parameters as project is not parametrized!\n";
				    }
				    
				    if (scheduleBuild(bot, project, delay, sender, parameters)) {
				        if (delay == 0) {
				            return msg + sender.getNickname() + ": job " + jobName + " build scheduled now";
				        } else {
				            return msg + sender.getNickname() + ": job " + jobName + " build scheduled with a quiet period of " +
                                    delay + " seconds";
				        }
                    } else {
                        return sender.getNickname() + ": job " + jobName + " scheduling failed or already in build queue";
                    }
				}
    		} else {
    			return giveSyntax(sender.getNickname(), args[0]);
    		}
		} else {
			return sender.getNickname() + ": Error, syntax is: '" + args[0] +  SYNTAX + "'";
		}
	}
	
	private String giveSyntax(String sender, String cmd) {
		return sender + ": syntax is: '" + cmd +  SYNTAX + "'";
	}

	public String getHelp() {
		return HELP;
	}

}
