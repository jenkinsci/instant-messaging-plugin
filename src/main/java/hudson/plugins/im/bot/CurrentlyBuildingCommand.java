package hudson.plugins.im.bot;

import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Queue.Executable;
import hudson.model.queue.SubTask;
import hudson.plugins.im.IMChat;
import hudson.plugins.im.IMException;
import hudson.plugins.im.IMMessage;
import hudson.plugins.im.Sender;

import java.util.Arrays;
import java.util.Collection;

import jenkins.model.Jenkins;

/**
 * CurrentlyBuilding command for instant messaging plugin.
 * 
 * Generates a list of jobs in progress.
 * 
 * @author Bjoern Kasteleiner
 */
@Extension
public class CurrentlyBuildingCommand extends BotCommand {

	@Override
	public Collection<String> getCommandNames() {
		return Arrays.asList("currentlyBuilding", "cb");
	}

	@Override
	public void executeCommand(Bot bot, IMChat chat, IMMessage message,
			Sender sender, String[] args) throws IMException {
		StringBuffer msg = new StringBuffer();
		msg.append("Currently building:");
		boolean currentlyJobsInProgess = false;
		for (Computer computer : Jenkins.getInstance().getComputers()) {
			for (Executor executor : computer.getExecutors()) {
				Executable currentExecutable = executor.getCurrentExecutable();
				if (currentExecutable != null) {
					currentlyJobsInProgess = true;
					
					SubTask task = currentExecutable.getParent();
					Item item = null;
					if (task instanceof Item) {
						item = (Item) task;
					}
					
					msg.append("\n- ");
					msg.append(computer.getDisplayName());
					msg.append("#");
					msg.append(executor.getNumber());
					msg.append(": ");
					msg.append(item != null ? item.getFullDisplayName() : task.getDisplayName());
					msg.append(" (Elapsed time: ");
					msg.append(Util.getTimeSpanString(executor.getElapsedTime()));
					msg.append(", Estimated remaining time: ");
					msg.append(executor.getEstimatedRemainingTime());
					msg.append(")");
				}
			}
		}

		if (!currentlyJobsInProgess) {
			msg.append("\n- No jobs are running.");
		}

		chat.sendMessage(msg.toString());
	}

	@Override
	public String getHelp() {
		return " - list jobs which are currently in progress";
	}

}
