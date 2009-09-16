package hudson.plugins.im;

import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.im.tools.Assert;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.MessageHelper;
import hudson.plugins.im.GroupChatIMMessageTarget;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The actual Publisher that sends notification-Messages out to the clients.
 * @author Uwe Schaefer
 *
 */
public abstract class IMPublisher extends Notifier implements BuildStep
{
	private static final Logger LOGGER = Logger.getLogger(IMPublisher.class.getName());
	
    private static final IMMessageTargetConverter CONVERTER = new DefaultIMMessageTargetConverter();
    
    private final List<IMMessageTarget> targets = new LinkedList<IMMessageTarget>();
    
    /**
     * @deprecated only left here to deserialize old configs
     */
    private hudson.plugins.jabber.NotificationStrategy notificationStrategy;
    
    private NotificationStrategy strategy;
    private final boolean notifyOnBuildStart;
    private final boolean notifySuspects;
    private final boolean notifyCulprits;
    private final boolean notifyFixers;
    private final String defaultIdSuffix;

    protected IMPublisher(final String targetsAsString, final String notificationStrategyString,
    		final boolean notifyGroupChatsOnBuildStart,
    		final boolean notifySuspects,
    		final boolean notifyCulprits,
    		final boolean notifyFixers,
    		String defaultIdSuffix) throws IMMessageTargetConversionException
    {
        Assert.isNotNull(targetsAsString, "Parameter 'targetsAsString' must not be null.");
        final String[] split = targetsAsString.split("\\s");
        final IMMessageTargetConverter conv = getIMMessageTargetConverter();
        for (final String fragment : split)
        {
            IMMessageTarget createIMMessageTarget;
            createIMMessageTarget = conv.fromString(fragment);
            if (createIMMessageTarget != null)
            {
                this.targets.add(createIMMessageTarget);
            }
        }

        NotificationStrategy strategy = NotificationStrategy.forDisplayName(notificationStrategyString);
        if (strategy == null) {
        	strategy = NotificationStrategy.STATECHANGE_ONLY;
        }
        this.strategy = strategy;
        
        this.notifyOnBuildStart = notifyGroupChatsOnBuildStart;
        this.notifySuspects = notifySuspects;
        this.notifyCulprits = notifyCulprits;
        this.notifyFixers = notifyFixers;
        this.defaultIdSuffix = Util.fixEmptyAndTrim(defaultIdSuffix);
    }
    
    /**
     * Returns a short name of the plugin to be used e.g. in log messages.
     */
    protected abstract String getPluginName();
    
    protected abstract IMConnection getIMConnection() throws IMException;

    protected IMMessageTargetConverter getIMMessageTargetConverter()
    {
        return IMPublisher.CONVERTER;
    }

    protected NotificationStrategy getNotificationStrategy()
    {
        return strategy;
    }

    private List<IMMessageTarget> getNotificationTargets()
    {
        return this.targets;
    }

    public final String getTargets()
    {
        final StringBuilder sb = new StringBuilder();
        for (final IMMessageTarget t : this.targets)
        {
        	if ((t instanceof GroupChatIMMessageTarget) && (! t.toString().contains("@conference."))) {
        		sb.append("*");
        	}
            sb.append(t.toString());
            sb.append(" ");
        }
        return sb.toString().trim();
    }
    
    public final String getStrategy() {
        return getNotificationStrategy().getDisplayName();
    }
    
    public final boolean getNotifyOnStart() {
    	return notifyOnBuildStart;
    }
    
    public final boolean getNotifySuspects() {
    	return notifySuspects;
    }
    
    public final boolean getNotifyCulprits() {
    	return notifyCulprits;
    }

    public final boolean getNotifyFixers() {
    	return notifyFixers;
    }
    
    protected void log(BuildListener listener, String message) {
    	listener.getLogger().append(getPluginName()).append(": ").append(message).append("\n");
    }

    @Override
    public boolean perform(final AbstractBuild<?,?> build, final Launcher launcher, final BuildListener buildListener)
            throws InterruptedException, IOException
    {
        Assert.isNotNull(build, "Parameter 'build' must not be null.");
        Assert.isNotNull(buildListener, "Parameter 'buildListener' must not be null.");
        if (getNotificationStrategy().notificationWanted(build)) {
            notifyChats(build, buildListener);
        }

        if (this.notifySuspects && BuildHelper.isFailureOrUnstable(build)) {
        	log(buildListener, "Notifying suspects");
        	final String message = "Oh no! You're suspected of having broken " + build.getProject().getName() + ": " + MessageHelper.getBuildURL(build);
        	
        	for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
        		try {
        			log(buildListener, "Sending notification to suspect: " + target.toString());
        			getIMConnection().send(target, message);
        		} catch (final Throwable e) {
        			log(buildListener, "There was an error sending suspect notification to: " + target.toString());
        		}
        	}
        }
        
        if (this.notifyCulprits && BuildHelper.isFailureOrUnstable(build)) {
        	log(buildListener, "Notifying culprits");
        	final String message = "You're still being suspected of having broken " + build.getProject().getName() + ": " + MessageHelper.getBuildURL(build);
        	
        	for (IMMessageTarget target : calculateIMTargets(getCulpritsOnly(build), buildListener)) {
        		try {
        			log(buildListener, "Sending notification to culprit: " + target.toString());
        			getIMConnection().send(target, message);
        		} catch (final Throwable e) {
        			log(buildListener, "There was an error sending suspect notification to: " + target.toString());
        		}
        	}
        }
        
        if (this.notifyFixers && BuildHelper.isFix(build)) {
        	buildListener.getLogger().append("Notifying fixers\n");
        	final String message = "Yippie! Seems you've fixed " + build.getProject().getName() + ": " + MessageHelper.getBuildURL(build);
        	
        	for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
        		try {
        			log(buildListener, "Sending notification to fixer: " + target.toString());
        			getIMConnection().send(target, message);
        		} catch (final Throwable e) {
        			log(buildListener, "There was an error sending fixer notification to: " + target.toString());
        		}
        	}
        }
        
        return true;
    }

    /**
     * Notify all registered chats about the build result.
     */
	private void notifyChats(final AbstractBuild<?, ?> build, final BuildListener buildListener) {
		final StringBuilder sb;
		if (BuildHelper.isFix(build)) {
			sb = new StringBuilder("Yippie, build fixed!\n");
		} else {
			sb = new StringBuilder();
		}
		sb.append("Project ").append(build.getProject().getName())
			.append(" build (").append(build.getNumber()).append("): ")
			.append(BuildHelper.getResultDescription(build)).append(" in ")
			.append(build.getTimestampString())
			.append(": ")
			.append(MessageHelper.getBuildURL(build));
		
		if (! build.getChangeSet().isEmptySet()) {
			boolean hasManyChangeSets = build.getChangeSet().getItems().length > 1;
			for (Entry entry : build.getChangeSet()) {
				sb.append("\n");
				if (hasManyChangeSets) {
					sb.append("* ");
				}
				sb.append(entry.getAuthor()).append(": ").append(entry.getMsg());
			}
		}
		final String msg = sb.toString();

		for (IMMessageTarget target : getNotificationTargets())
		{
		    try
		    {
		        log(buildListener, "Sending notification to: " + target.toString());
		        getIMConnection().send(target, msg);
		    }
		    catch (final Throwable e)
		    {
		        log(buildListener, "There was an error sending notification to: " + target.toString());
		    }
		}
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.Publisher#prebuild(hudson.model.Build, hudson.model.BuildListener)
	 */
	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener buildListener) {
		try {
			if (notifyOnBuildStart) {
				final StringBuilder sb = new StringBuilder("Starting build ").append(build.getNumber())
					.append(" for job ").append(build.getProject().getName());
				if (build.getPreviousBuild() != null) {
					sb.append(" (previous build: ").append(build.getPreviousBuild().getResult().toString().toLowerCase());
					if (build.getPreviousBuild().getResult().isWorseThan(Result.SUCCESS)) {
						sb.append(" -- last ").append(build.getPreviousNotFailedBuild().getResult().toString().toLowerCase())
						.append(" #").append(build.getPreviousNotFailedBuild().getNumber())
						.append(" ").append(build.getPreviousNotFailedBuild().getTimestampString()).append(" ago");
					}
					sb.append(")");
				}
				final String msg = sb.toString();
				for (final IMMessageTarget target : getNotificationTargets()) {
					// only notify group chats
					if (target instanceof GroupChatIMMessageTarget) {
		                try {
		                    getIMConnection().send(target, msg);
		                } catch (final Throwable e) {
		                    log(buildListener, "There was an error sending notification to: " + target.toString());
		                }
					}
	            }
			}
		} catch (Throwable t) {
			// ignore: never, ever cancel a build because a notification fails
            log(buildListener, "There was an error in the Jabber plugin: " + t.toString());
		}
		return true;
	}
	
	private static Set<User> getCommitters(AbstractBuild<?, ?> build) {
		Set<User> committers = new HashSet<User>();
		ChangeLogSet<? extends Entry> changeSet = build.getChangeSet();
		for (Entry entry : changeSet) {
			committers.add(entry.getAuthor());
		}
		return committers;
	}
	
	/**
	 * Returns the culprits WITHOUT the committers to the current build.
	 */
	private static Set<User> getCulpritsOnly(AbstractBuild<?, ?> build) {
		Set<User> culprits = new HashSet<User>(build.getCulprits());
		culprits.removeAll(getCommitters(build));
		return culprits;
	}
	
	private Collection<IMMessageTarget> calculateIMTargets(Set<User> targets, BuildListener listener) {
		Set<IMMessageTarget> suspects = new HashSet<IMMessageTarget>();
		
		LOGGER.fine("Default Suffix: " + defaultIdSuffix);
		
		for (User target : targets) {
			LOGGER.fine("Possible target: " + target.getId());
            String jabberId = getConfiguredIMId(target);
			if (jabberId == null && this.defaultIdSuffix != null) {
                jabberId = target.getId() + defaultIdSuffix;
            }

            if (jabberId != null) {
                try {
                    suspects.add(CONVERTER.fromString(jabberId));
                } catch (final IMMessageTargetConversionException e) {
                    log(listener, "Invalid Jabber ID: " + jabberId);
                }
            } else {
            	log(listener, "No Jabber ID found for: " + target.getId());
            }
		}
		return suspects;
	}

    @Override
    public abstract BuildStepDescriptor<Publisher> getDescriptor();
	
    
    private Object readResolve() {
    	if (this.strategy == null && this.notificationStrategy != null) {
    		this.strategy = NotificationStrategy.valueOf(this.notificationStrategy.name());
    		this.notificationStrategy = null;
    	}
    	return this;
    }
    
    protected abstract String getConfiguredIMId(User user);
}
