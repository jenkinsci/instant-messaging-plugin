package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.Util;
import hudson.model.Build;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue.Executable;
import hudson.model.Queue.Task;
import hudson.model.queue.SubTask;
import hudson.model.Run;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.Sender;
import jenkins.model.JenkinsLocationConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution.PlaceholderTask;

/**
 * CurrentlyBuilding command for instant messaging plugin.
 *
 * Generates a list of jobs in progress.
 *
 * @author Bjoern Kasteleiner
 */
@Extension
public class CurrentlyBuildingCommand extends BotCommand {
    private static final String SYNTAX = " [@] [?] [~ regex pattern]";
    private static final String HELP = SYNTAX + " - list jobs which are currently in progress, with optional '@' display of URLs to the build (console log if possible) and/or '~ regex' filter on reported lines; '?' enables debugging of the command itself";

    @Override
    public Collection<String> getCommandNames() {
        return Arrays.asList("currentlyBuilding", "cb");
    }

    @Override
    public void executeCommand(Bot bot, IMChat chat, IMMessage message,
            Sender sender, String[] args) throws IMException {
        StringBuffer msg = new StringBuffer();
        String filterRegex = null;
        Pattern filterPattern = null;
        boolean reportUrls = false;
        boolean cbDebug = false;
        // We are interested in args to the command, if any,
        // so starting from args[1] when (args.length >= 2)
        argsloop: // label for break to know which statement to abort
        for (int a = 1 ; args.length > a; a++) {
            if (cbDebug) { chat.sendMessage("a=" + a + "  arg='" + args[a] + "' len=" + args.length +"\n"); }
            switch (args[a]) {
                case "@":
                    msg.append("\n- NOTE: got @ argument for currentlyBuilding: will add URLs to reported strings");
                    reportUrls = true;
                    break;
                case "?":
                    msg.append("\n- NOTE: got ? argument for currentlyBuilding: will add debug about detected Executable and SubTask class objects");
                    cbDebug = true;
                    break;
                case "~": // the rest of line is the regex expression
                    if ( (args.length - a) < 1) {
                        msg.append("\n- WARNING: got ~ filtering argument for currentlyBuilding, but no filter value - so none was applied\n");
                        break argsloop;
                    }
                    for (int i = (a + 1); i < args.length; i++) {
                        if ( (a + 1) == i) {
                            // avoid appending to null
                            filterRegex = args[i];
                        } else {
                            // We can not really assume what the user
                            // entered if there were e.g. several
                            // whitespaces trimmed by line-parser.
                            // So if they meant modifiers (brackets,
                            // counts), they should spell them out.
                            filterRegex += " " + args[i];
                        }
                    }
                    if ( filterRegex == null ) {
                        msg.append("\n- WARNING: got ~ filtering argument for currentlyBuilding, but failed to extract a filter value (maybe a bug in instant-messaging-plugin) - so none was applied\n");
                    } else {
                        msg.append("\n- NOTE: got ~ filtering argument for currentlyBuilding: applying regex filter to reported strings: " + filterRegex);
                        filterPattern = Pattern.compile(filterRegex);
                    }
                    break argsloop;
                default:
                    msg.append("\n- WARNING: got unsupported argument '" + args[a] + "' for currentlyBuilding, ignored; no filter was applied\n");
                    break;
            }
        }

        String rootUrl = null;
        if (reportUrls) {
            JenkinsLocationConfiguration cfg = JenkinsLocationConfiguration.get();
            if (cfg != null) {
                rootUrl = cfg.getUrl();
            }
            if (rootUrl == null) {
                msg.append("\n- WARNING: Could not determine Jenkins URL for reporting.\n");
            } else {
                // Ensure one slash at the end of hostname/port
                // when we concatenate with relative job URLs
                // later (no leading slash there).
                rootUrl = rootUrl.replaceFirst("/*$", "") + "/";
            }
        }

        int countJobsInProgess = 0;
        int countJobsInPattern = 0;
        for (Computer computer : Jenkins.getInstance().getComputers()) {
            for (Executor executor : computer.getExecutors()) {
                Executable currentExecutable = executor.getCurrentExecutable();
                if (currentExecutable != null) {
                    countJobsInProgess++;

                    Build currentExecutableBuild = null;
                    if (currentExecutable instanceof Build) {
                        currentExecutableBuild = (Build) currentExecutable;
                    }

                    SubTask task = currentExecutable.getParent();
                    Item item = null;
                    if (task instanceof Item) {
                        item = (Item) task;
                    }

                    PlaceholderTask placeholderTask = null;
                    if (task instanceof PlaceholderTask) {
                        // e.g. a part of pipeline
                        placeholderTask = (PlaceholderTask) task;
                    }

                    StringBuffer msgLine = new StringBuffer();

                    msgLine.append(computer.getDisplayName());
                    msgLine.append("#");
                    msgLine.append(executor.getNumber());
                    msgLine.append(": ");
                    if (item == null) {
                        // Display name of a running subtask (one or more per build,
                        // depending on parallelism) includes its build number
                        // e.g. in pipeline originated items.
                        if (cbDebug) { msgLine.append("task.getDisplayName()= "); }
                        msgLine.append(task.getDisplayName());
                    } else {
                        if (currentExecutableBuild != null) {
                            // A legacy freestyle job build is running.
                            // Its higher-level Executable has the number.
                            if (cbDebug) { msgLine.append("currentExecutableBuild.getFullDisplayName()= "); }
                            msgLine.append(currentExecutableBuild.getFullDisplayName());
                        } else
                        if (task instanceof Run) {
                            if (cbDebug) { msgLine.append(" RunTask_data_of_Item= "); }
                            Run r = (Run) task;
                            msgLine.append(item.getFullDisplayName());
                            msgLine.append("#");
                            msgLine.append(r.getNumber());
                        }
                    }

                    if (reportUrls) {
                        String relativeUrl = null;
                        if (currentExecutableBuild != null) {
                            if (cbDebug) { msgLine.append(" URL:currExec= "); }
                            relativeUrl = currentExecutableBuild.getUrl();
                        }
                        if ((relativeUrl == null || relativeUrl.equals("")) && placeholderTask != null) {
                            if (cbDebug) { msgLine.append(" URL:phTask= "); }
                            relativeUrl = placeholderTask.getUrl();
                        }
                        if ((relativeUrl == null || relativeUrl.equals("")) && item != null) {
                            if (cbDebug) { msgLine.append(" URL:item= "); }
                            relativeUrl = item.getUrl();
                        }
                        if (relativeUrl == null || relativeUrl.equals("")) {
                            // a SubTask has no getUrl() of its own
                            if (cbDebug) { msgLine.append(" URL:ownerTask= "); }
                            Task t = (Task) task.getOwnerTask();
                            relativeUrl = t.getUrl();
                        }
                        if (relativeUrl != null && !relativeUrl.equals("")) {
                            Pattern p = Pattern.compile("/[0-9]+/*$"); // BuildID component in URL
                            Matcher m = p.matcher(relativeUrl);
                            if (m.find()) {
                                relativeUrl = relativeUrl.replaceFirst("/*$", "") + "/console";
                            }
                            msgLine.append(" @ ");
                            msgLine.append(rootUrl + relativeUrl);
                        }
                    }

                    if (filterPattern != null) {
                        Matcher matcher = filterPattern.matcher(msgLine);
                        if (!matcher.find()) {
                            continue;
                        }
                        // We have a regex hit, report it
                        countJobsInPattern++;
                    }

                    msg.append("\n- ");
                    msg.append(msgLine);
                    msg.append(" (Elapsed time: ");
                    msg.append(Util.getTimeSpanString(executor.getElapsedTime()));
                    msg.append(", Estimated remaining time: ");
                    msg.append(executor.getEstimatedRemainingTime());
                    msg.append(")");

                    if (cbDebug) {
                        msg.append("\n=== currExec class: ");
                        msg.append(Arrays.asList(currentExecutable.getClass().getName()));

                        msg.append("\n=== currExec interfaces: ");
                        msg.append(Arrays.asList(currentExecutable.getClass().getInterfaces()));

                        msg.append("\n=== currExec classes: ");
                        msg.append(Arrays.asList(currentExecutable.getClass().getClasses()));

                        msg.append("\n=== currTask class: ");
                        msg.append(Arrays.asList(task.getClass().getName()));

                        msg.append("\n=== currTask interfaces: ");
                        msg.append(Arrays.asList(task.getClass().getInterfaces()));

                        msg.append("\n=== currTask classes: ");
                        msg.append(Arrays.asList(task.getClass().getClasses()));

                        msg.append("\n");
                    }
                }
            }
        }

        if (countJobsInProgess == 0) {
            msg.append("\n- No jobs are running.");
        } else if (countJobsInPattern == 0 && filterPattern != null) {
            msg.append("\n- None of the running jobs matched the filter.");
        }

        if (filterPattern != null) {
            msg.insert(0, "Currently building (" + countJobsInProgess +
                " items total, of which " + countJobsInPattern +
                " items matched the filter):");
        } else {
            msg.insert(0, "Currently building (" + countJobsInProgess +
                " items):");
        }

        chat.sendMessage(msg.toString());
    }

    private String giveSyntax(String sender, String cmd) {
        return sender + ": syntax is: '" + cmd +  SYNTAX + "'";
    }

    @Override
    public String getHelp() {
        return HELP;
    }

}
