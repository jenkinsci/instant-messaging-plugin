package hudson.plugins.im;

import static hudson.plugins.im.tools.BuildHelper.getProjectName;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.Fingerprint.RangeSet;
import hudson.plugins.im.tools.Assert;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.ExceptionHelper;
import hudson.plugins.im.tools.MessageHelper;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The actual Publisher that sends notification-Messages out to the clients.
 * 
 * @author Uwe Schaefer
 * @author Christoph Kutzinski
 */
public abstract class IMPublisher extends Notifier implements BuildStep
{
	private static final Logger LOGGER = Logger.getLogger(IMPublisher.class.getName());
	
    private List<IMMessageTarget> targets;
    
    /**
     * @deprecated only left here to deserialize old configs
     */
    @Deprecated
	private hudson.plugins.jabber.NotificationStrategy notificationStrategy;
    
    private NotificationStrategy strategy;
    private final boolean notifyOnBuildStart;
    private final boolean notifySuspects;
    private final boolean notifyCulprits;
    private final boolean notifyFixers;
    private final boolean notifyUpstreamCommitters;
    
    /**
     * @deprecated Only for deserializing old instances
     */
    @SuppressWarnings("unused")
    @Deprecated
    private transient String defaultIdSuffix;

    protected IMPublisher(List<IMMessageTarget> defaultTargets,
    		String notificationStrategyString,
    		boolean notifyGroupChatsOnBuildStart,
    		boolean notifySuspects,
    		boolean notifyCulprits,
    		boolean notifyFixers,
    		boolean notifyUpstreamCommitters)
    {
    	if (defaultTargets != null) {
    		this.targets = defaultTargets;
    	} else {
    		this.targets = Collections.emptyList();
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
        this.notifyUpstreamCommitters = notifyUpstreamCommitters;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        // notifyUpstreamCommitters needs the fingerprints to be generated
        // which seems to happen quite late in the build
        return this.notifyUpstreamCommitters;
    }
    
    /**
     * Returns a short name of the plugin to be used e.g. in log messages.
     */
    protected abstract String getPluginName();
    
    protected abstract IMConnection getIMConnection() throws IMException;

    protected NotificationStrategy getNotificationStrategy() {
        return strategy;
    }
    
    protected void setNotificationStrategy(NotificationStrategy strategy) {
    	this.strategy = strategy;
    }

    /**
     * Returns the notification targets configured on a per-job basis.
     */
    public List<IMMessageTarget> getNotificationTargets() {
        return this.targets;
    }
    
    /**
     * Returns the notification target which should actually be used for notification.
     * 
     * Differs from {@link #getNotificationTargets()} because it also takes
     * {@link IMPublisherDescriptor#getDefaultTargets()} into account!
     */
    protected List<IMMessageTarget> calculateTargets() {
    	if (getNotificationTargets() != null && getNotificationTargets().size() > 0) {
    		return getNotificationTargets();
    	}
    	
    	return ((IMPublisherDescriptor)getDescriptor()).getDefaultTargets();
    }

    /**
     * Returns the notification targets as a string suitable for
     * display in the settings page.
     *
     * Returns an empty string if no targets are set.
     */
    public String getTargets() {
    	if (this.targets == null) {
    		return "";
    	}

        final StringBuilder sb = new StringBuilder();
        for (final IMMessageTarget t : this.targets) {
            sb.append(getIMDescriptor().getIMMessageTargetConverter().toString(t));
            sb.append(" ");
        }
        return sb.toString().trim();
    }
	
    @Deprecated
    protected void setTargets(String targetsAsString) throws IMMessageTargetConversionException {
    	this.targets = new LinkedList<IMMessageTarget>();
    	
        final String[] split = targetsAsString.split("\\s");
        final IMMessageTargetConverter conv = getIMDescriptor().getIMMessageTargetConverter();
        for (final String fragment : split)
        {
            IMMessageTarget createIMMessageTarget;
            createIMMessageTarget = conv.fromString(fragment);
            if (createIMMessageTarget != null)
            {
                this.targets.add(createIMMessageTarget);
            }
        }
	}
    
    /**
     * @deprecated Should only be used to deserialize old instances
     */
    @Deprecated
	protected void setNotificationTargets(List<IMMessageTarget> targets) {
    	if (targets != null) {
    		this.targets = targets;
    	} else {
    		this.targets = Collections.emptyList();
    	}
    }
    
    /**
     * Returns the selected notification strategy as a string
     * suitable for display.
     */
    public final String getStrategy() {
        return getNotificationStrategy().getDisplayName();
    }
    
    /**
     * Specifies if the starting of builds should be notified to
     * the registered chat rooms.
     */
    public final boolean getNotifyOnStart() {
    	return notifyOnBuildStart;
    }
    
    /**
     * Specifies if committers to failed builds should be informed about
     * build failures.
     */
    public final boolean getNotifySuspects() {
    	return notifySuspects;
    }
    
    /**
     * Specifies if culprits - i.e. committers to previous already failing
     * builds - should be informed about subsequent build failures.
     */
    public final boolean getNotifyCulprits() {
    	return notifyCulprits;
    }

    /**
     * Specifies if 'fixers' should be informed about
     * fixed builds.
     */
    public final boolean getNotifyFixers() {
    	return notifyFixers;
    }
    
    /**
     * Specifies if upstream committers should be informed about
     * build failures.
     */
    public final boolean getNotifyUpstreamCommitters() {
        return notifyUpstreamCommitters;
    }
    
    /**
     * Logs message to the build listener's logger.
     */
    protected void log(BuildListener listener, String message) {
    	listener.getLogger().append(getPluginName()).append(": ").append(message).append("\n");
    }

    @Override
    public boolean perform(final AbstractBuild<?,?> build, final Launcher launcher, final BuildListener buildListener)
            throws InterruptedException
    {
        Assert.isNotNull(build, "Parameter 'build' must not be null.");
        Assert.isNotNull(buildListener, "Parameter 'buildListener' must not be null.");
        if (getNotificationStrategy().notificationWanted(build)) {
            notifyChats(build, buildListener);
        }

        if (BuildHelper.isStillFailureOrUnstable(build)) {
            if (this.notifySuspects) {
            	log(buildListener, "Notifying suspects");
            	final String message = "Build " + getProjectName(build) +
            	    " is " + BuildHelper.getResultDescription(build) + ": " + MessageHelper.getBuildURL(build);
            	
            	for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
            		try {
            			log(buildListener, "Sending notification to suspect: " + target.toString());
            			sendNotification(message, target, buildListener);
            		} catch (final Throwable e) {
            			log(buildListener, "There was an error sending suspect notification to: " + target.toString());
            		}
            	}
            }
            
            if (this.notifyCulprits) {
            	log(buildListener, "Notifying culprits");
            	final String message = "You're still being suspected of having broken " + getProjectName(build) + ": " + MessageHelper.getBuildURL(build);
            	
            	for (IMMessageTarget target : calculateIMTargets(getCulpritsOnly(build), buildListener)) {
            		try {
            			log(buildListener, "Sending notification to culprit: " + target.toString());
            			sendNotification(message, target, buildListener);
            		} catch (final Throwable e) {
            			log(buildListener, "There was an error sending culprit notification to: " + target.toString());
            		}
            	}
            }
        } else if (BuildHelper.isFailureOrUnstable(build)) {
            boolean committerNotified = false;
            if (this.notifySuspects) {
                log(buildListener, "Notifying suspects");
                String message = "Oh no! You're suspected of having broken " + getProjectName(build) + ": " + MessageHelper.getBuildURL(build);
                
                for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
                    try {
                        log(buildListener, "Sending notification to suspect: " + target.toString());
                        sendNotification(message, target, buildListener);
                        committerNotified = true;
                    } catch (final Throwable e) {
                        log(buildListener, "There was an error sending suspect notification to: " + target.toString());
                    }
                }
            }
            
            if (this.notifyUpstreamCommitters && !committerNotified) {
                notifyUpstreamCommitters(build, buildListener);
            }
        }
        
        if (this.notifyFixers && BuildHelper.isFix(build)) {
        	buildListener.getLogger().append("Notifying fixers\n");
        	final String message = "Yippie! Seems you've fixed " + getProjectName(build) + ": " + MessageHelper.getBuildURL(build);
        	
        	for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
        		try {
        			log(buildListener, "Sending notification to fixer: " + target.toString());
        			sendNotification(message, target, buildListener);
        		} catch (final Throwable e) {
        			log(buildListener, "There was an error sending fixer notification to: " + target.toString());
        		}
        	}
        }
        
        return true;
    }

	private void sendNotification(String message, IMMessageTarget target, BuildListener buildListener)
			throws IMException {
		IMConnection imConnection = getIMConnection();
		if (imConnection instanceof DummyConnection) {
			// quite hacky
			log(buildListener, "[ERROR] not connected. Cannot send message to '" + target + "'");
		} else {
			getIMConnection().send(target, message);
		}
	}

    /**
     * Looks for committers in the direct upstream builds and notifies them.
     * If no committers are found in the next higher level, look one level higher.
     * Repeat if necessary. 
     */
    @SuppressWarnings("unchecked")
	private void notifyUpstreamCommitters(final AbstractBuild<?, ?> build,
			final BuildListener buildListener) {
		boolean committerNotified = false;
		Map<AbstractProject, Integer> upstreamBuilds = build.getUpstreamBuilds();
		
		while (!committerNotified && !upstreamBuilds.isEmpty()) {
			Map<AbstractProject, Integer> currentLevel = upstreamBuilds;
			// new map for the builds one level higher up:
			upstreamBuilds = new HashMap<AbstractProject, Integer>();
			
		    for (Map.Entry<AbstractProject, Integer> entry : currentLevel.entrySet()) {
		        AbstractBuild<?, ?> upstreamBuild = (AbstractBuild<?, ?>) entry.getKey().getBuildByNumber(entry.getValue());
		        
		        if (upstreamBuild != null) {
		            
		            if (! downstreamIsFirstInRangeTriggeredByUpstream(upstreamBuild, build)) {
		                continue;
		            }
		            
			        Set<User> committers = getCommitters(upstreamBuild);
			        
			        String message = "Attention! Your change in " + getProjectName(upstreamBuild)
			        + ": " + MessageHelper.getBuildURL(upstreamBuild)
			        + " *might* have broken the downstream job " + getProjectName(build) + ": " + MessageHelper.getBuildURL(build)
			        + "\nPlease have a look!";
			        
			        for (IMMessageTarget target : calculateIMTargets(committers, buildListener)) {
			            try {
			                log(buildListener, "Sending notification to upstream committer: " + target.toString());
			                sendNotification(message, target, buildListener);
			                committerNotified = true;
			            } catch (final Throwable e) {
			                log(buildListener, "There was an error sending upstream committer notification to: " + target.toString());
			            }
			        }
		        }
		        
		        if (!committerNotified) {
		        	upstreamBuilds.putAll(upstreamBuild.getUpstreamBuilds());
		        }
		    }
		}
	}

    /**
     * Determines if downstreamBuild is the 1st build of the downstream project
     * which has a dependency to the upstreamBuild.
     */
    //@Bug(6712)
    private boolean downstreamIsFirstInRangeTriggeredByUpstream(
            AbstractBuild<?, ?> upstreamBuild, AbstractBuild<?, ?> downstreamBuild) {
        RangeSet rangeSet = upstreamBuild.getDownstreamRelationship(downstreamBuild.getProject());
        
        if (rangeSet.isEmpty()) {
            // should not happen
            LOGGER.warning("Range set is empty. Upstream " + upstreamBuild + ", downstream " + downstreamBuild);
            return false;
        }
        
        if (rangeSet.min() == downstreamBuild.getNumber()) {
            return true;
        }
        return false;
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
		sb.append("Project ").append(getProjectName(build))
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

		for (IMMessageTarget target : calculateTargets())
		{
		    try {
		        log(buildListener, "Sending notification to: " + target.toString());
		        sendNotification(msg, target, buildListener);
		    } catch (final Throwable t) {
		        log(buildListener, "There was an error sending notification to: " + target.toString() + "\n" + ExceptionHelper.dump(t));
		    }
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener buildListener) {
		try {
			if (notifyOnBuildStart) {
				final StringBuilder sb = new StringBuilder("Starting build ").append(build.getNumber())
					.append(" for job ").append(getProjectName(build));

				if (build.getPreviousBuild() != null) {
					sb.append(" (previous build: ")
						.append(BuildHelper.getResultDescription(build.getPreviousBuild()));

					if (build.getPreviousBuild().getResult().isWorseThan(Result.SUCCESS)) {
						AbstractBuild<?, ?> lastSuccessfulBuild = build.getPreviousNotFailedBuild();
						if (lastSuccessfulBuild != null) {
							sb.append(" -- last ").append(BuildHelper.getResultDescription(lastSuccessfulBuild))
								.append(" #").append(lastSuccessfulBuild.getNumber())	
								.append(" ").append(lastSuccessfulBuild.getTimestampString()).append(" ago");
						}
					}
					sb.append(")");
				}
				final String msg = sb.toString();
				for (final IMMessageTarget target : calculateTargets()) {
					// only notify group chats
					if (target instanceof GroupChatIMMessageTarget) {
		                try {
		                    sendNotification(msg, target, buildListener);
		                } catch (final Throwable e) {
		                    log(buildListener, "There was an error sending notification to: " + target.toString());
		                }
					}
	            }
			}
		} catch (Throwable t) {
			// ignore: never, ever cancel a build because a notification fails
            log(buildListener, "There was an error in the IM plugin: " + ExceptionHelper.dump(t));
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
		
		String defaultIdSuffix = ((IMPublisherDescriptor)getDescriptor()).getDefaultIdSuffix();
		LOGGER.fine("Default Suffix: " + defaultIdSuffix);
		
		for (User target : targets) {
			LOGGER.fine("Possible target: " + target.getId());
            String imId = getConfiguredIMId(target);
			if (imId == null && defaultIdSuffix != null) {
                imId = target.getId() + defaultIdSuffix;
            }

            if (imId != null) {
                try {
                    suspects.add(getIMDescriptor().getIMMessageTargetConverter().fromString(imId));
                } catch (final IMMessageTargetConversionException e) {
                    log(listener, "Invalid IM ID: " + imId);
                }
            } else {
            	log(listener, "No IM ID found for: " + target.getId());
            }
		}
		return suspects;
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
    public abstract BuildStepDescriptor<Publisher> getDescriptor();
	
    // migrate old JabberPublisher instances
    private Object readResolve() {
    	if (this.strategy == null && this.notificationStrategy != null) {
    		this.strategy = NotificationStrategy.valueOf(this.notificationStrategy.name());
    		this.notificationStrategy = null;
    	}
    	return this;
    }
    
    protected final IMPublisherDescriptor getIMDescriptor() {
    	return (IMPublisherDescriptor) getDescriptor();
    }
    
    /**
     * Returns the instant-messaging ID which is configured for a Hudson user
     * (e.g. via a {@link UserProperty}) or null if there's nothing configured for
     * him/her.
     */
    protected abstract String getConfiguredIMId(User user);
}
