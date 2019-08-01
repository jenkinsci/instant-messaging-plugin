/*
 * Created on Apr 22, 2007
 */
package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.plugins.im.IMCause;
import hudson.plugins.im.Sender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;

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

    /**
     * @return whether the build was actually scheduled
     */
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
            AbstractProject<?, ?> project = getJobProvider().getJobByNameOrDisplayName(jobName);
            if (project != null) {

                String checkPermission = checkPermission(sender, project);
                if (checkPermission != null) {
                    return checkPermission;
                }

                StringBuilder reply = new StringBuilder();
                if (!project.isBuildable()) {
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

                        if (parametersStartIndex < args.length) {
                            String[] potentialParameters = (String[]) ArrayUtils.subarray(args, parametersStartIndex,args.length);
                            parameters = parseBuildParameters(potentialParameters, project, reply);
                        }
                    }

                    if (scheduleBuild(bot, project, delay, sender, parameters)) {
                        if (delay == 0) {
                            return reply.append(sender.getNickname() + ": job " + jobName + " build scheduled now").toString();
                        } else {
                            return reply.append(sender.getNickname() + ": job " + jobName + " build scheduled with a quiet period of " +
                                    delay + " seconds").toString();
                        }
                    } else {
                        // probably already queued
                        Queue.Item queueItem = project.getQueueItem();
                        if (queueItem != null) {
                            return sender.getNickname() + ": job " + jobName + " is already in the build queue (" + queueItem.getWhy() + ")";
                        } else {
                            // could be race condition (build left build-queue while we were checking) or other reason
                            return sender.getNickname() + ": job " + jobName + " scheduling failed or already in build queue";
                        }
                    }
                }
            } else {
                return giveSyntax(sender.getNickname(), args[0]);
            }
        } else {
            return sender.getNickname() + ": Error, syntax is: '" + args[0] +  SYNTAX + "'";
        }
    }

    List<ParameterValue> parseBuildParameters(String[] args,
            AbstractProject<?, ?> project, StringBuilder commandReply) {

        if (args.length > 0 && !project.isParameterized()) {
            commandReply.append("Ignoring parameters as project is not parametrized!\n");
            return Collections.emptyList();
        } else if (!project.isParameterized()) {
            return Collections.emptyList();
        }

        // parse possible parameters from the command
        Map<String, String> parsedParameters = new HashMap<String, String>();
        for (int i=0; i < args.length; i++) {
            String[] split = args[i].split("=");
            if (split.length == 2) {
                parsedParameters.put(split[0], split[1]);
            } else {
                commandReply.append("Unparseable parameter: " + args[i] + "\n");
            }
        }

        List<ParameterValue> parameters = new ArrayList<ParameterValue>();
        ParametersDefinitionProperty propDefs = project.getProperty(ParametersDefinitionProperty.class);
        for (ParameterDefinition pd : propDefs.getParameterDefinitions()) {
            if (pd.getName() != null && parsedParameters.containsKey(pd.getName())) {
                if (pd instanceof SimpleParameterDefinition) {
                    SimpleParameterDefinition spd = (SimpleParameterDefinition) pd;
                    parameters.add(spd.createValue(parsedParameters.get(pd.getName())));
                } else {
                    commandReply.append("Unsupported parameter type " + pd.getClass().getSimpleName()
                            + " for parameter " + pd.getName() + "!\n");
                }
            } else {
                ParameterValue pv = pd.getDefaultParameterValue();
                if (pv != null) {
                    parameters.add(pv);
                }
            }
        }
        return parameters;
    }

    private String checkPermission(Sender sender, AbstractProject<?, ?> project) {
        if (!project.hasPermission(Item.BUILD)) {
            return sender.getNickname() + ": you're not allowed to build job " + project.getDisplayName() + "!";
        }
        return null;
    }

    private String giveSyntax(String sender, String cmd) {
        return sender + ": syntax is: '" + cmd +  SYNTAX + "'";
    }

    public String getHelp() {
        return HELP;
    }


}
