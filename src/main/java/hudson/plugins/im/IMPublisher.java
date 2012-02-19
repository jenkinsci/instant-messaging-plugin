package hudson.plugins.im;

import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.BuildListener;
import hudson.model.UserProperty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.User;
import hudson.plugins.im.build_notify.BuildToChatNotifier;
import hudson.plugins.im.build_notify.DefaultBuildToChatNotifier;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.ExceptionHelper;
import hudson.plugins.im.tools.BuildHelper.ExtResult;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.util.Assert;

import com.google.common.collect.Lists;

/**
 * The actual Publisher which sends notification messages out to the clients.
 * 
 * @author Uwe Schaefer
 * @author Christoph Kutzinski
 */
public abstract class IMPublisher extends Notifier implements BuildStep, MatrixAggregatable
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
    private BuildToChatNotifier buildToChatNotifier;
    private MatrixJobMultiplier matrixMultiplier = MatrixJobMultiplier.ONLY_CONFIGURATIONS;
    
    /**
     * @deprecated Only for deserializing old instances
     */
    @SuppressWarnings("unused")
    @Deprecated
    private transient String defaultIdSuffix;

    /**
     * @deprecated
     *      as of 1.9. Use {@link #IMPublisher(List, String, boolean, boolean, boolean, boolean, boolean, BuildToChatNotifier)}
     *      instead.
     */
    @Deprecated
	protected IMPublisher(List<IMMessageTarget> defaultTargets,
    		String notificationStrategyString,
    		boolean notifyGroupChatsOnBuildStart,
    		boolean notifySuspects,
    		boolean notifyCulprits,
    		boolean notifyFixers,
    		boolean notifyUpstreamCommitters) {
        this(defaultTargets,notificationStrategyString,notifyGroupChatsOnBuildStart,notifySuspects,notifyCulprits,
                notifyFixers,notifyUpstreamCommitters,new DefaultBuildToChatNotifier(), MatrixJobMultiplier.ALL);
    }

    protected IMPublisher(List<IMMessageTarget> defaultTargets,
    		String notificationStrategyString,
    		boolean notifyGroupChatsOnBuildStart,
    		boolean notifySuspects,
    		boolean notifyCulprits,
    		boolean notifyFixers,
    		boolean notifyUpstreamCommitters,
            BuildToChatNotifier buildToChatNotifier,
            MatrixJobMultiplier matrixMultiplier)
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
        this.buildToChatNotifier = buildToChatNotifier;
        this.matrixMultiplier = matrixMultiplier;
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

    public BuildToChatNotifier getBuildToChatNotifier() {
        return buildToChatNotifier;
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
    public boolean getNotifyOnStart() {
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
            throws InterruptedException, IOException {
        Assert.notNull(build, "Parameter 'build' must not be null.");
        Assert.notNull(buildListener, "Parameter 'buildListener' must not be null.");
        
        if (build.getProject() instanceof MatrixConfiguration) {
            if (getMatrixNotifier() == MatrixJobMultiplier.ONLY_CONFIGURATIONS
                || getMatrixNotifier() == MatrixJobMultiplier.ALL) {
                notifyOnBuildEnd(build, buildListener);
            }
        } else {
            notifyOnBuildEnd(build, buildListener);
        }
        
        return true;
    }

    /**
     * Sends notification at build end including maybe notifications of culprits, fixers or so.
     */
    /* package for testing */ void notifyOnBuildEnd(final AbstractBuild<?, ?> build,
            final BuildListener buildListener) throws IOException,
            InterruptedException {
        if (getNotificationStrategy().notificationWanted(build)) {
            notifyChats(build, buildListener);
        }

        if (BuildHelper.isStillFailureOrUnstable(build) || BuildHelper.getExtendedResult(build) == ExtResult.NOW_UNSTABLE) {
            if (this.notifySuspects) {
            	log(buildListener, "Notifying suspects");
            	final String message = getBuildToChatNotifier().suspectMessage(this, build, buildListener, false);
            	
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
            	final String message = getBuildToChatNotifier().culpritMessage(this, build, buildListener);
            	
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
                String message = getBuildToChatNotifier().suspectMessage(this, build, buildListener, true);
                
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
        	final String message = getBuildToChatNotifier().fixerMessage(this, build, buildListener);
        	
        	for (IMMessageTarget target : calculateIMTargets(getCommitters(build), buildListener)) {
        		try {
        			log(buildListener, "Sending notification to fixer: " + target.toString());
        			sendNotification(message, target, buildListener);
        		} catch (final Throwable e) {
        			log(buildListener, "There was an error sending fixer notification to: " + target.toString());
        		}
        	}
        }
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
     * If no committers are found in the immediate upstream builds, then look one level higher.
     * Repeat until a committer is found or no more upstream builds are found. 
     */
	private void notifyUpstreamCommitters(final AbstractBuild<?, ?> build,
			final BuildListener buildListener) {
        
        Map<User, AbstractBuild<?,?>> committers = getNearestUpstreamCommitters(build);
			        
        
        for (Map.Entry<User, AbstractBuild<?, ?>> entry : committers.entrySet()) {
            String message = getBuildToChatNotifier().upstreamCommitterMessage(this, build, buildListener, entry.getValue());
            
            IMMessageTarget target = calculateIMTarget(entry.getKey(), buildListener);
            try {
                log(buildListener, "Sending notification to upstream committer: " + target.toString());
                sendNotification(message, target, buildListener);
            } catch (final Throwable e) {
                log(buildListener, "There was an error sending upstream committer notification to: " + target.toString());
            }
        }
	}
    
	/**
     * Looks for committers in the direct upstream builds.
     * If no committers are found in the immediate upstream builds, then look one level higher.
     * Repeat until a committer is found or no more upstream builds are found. 
     */
    @SuppressWarnings("rawtypes")
    Map<User, AbstractBuild<?,?>> getNearestUpstreamCommitters(AbstractBuild<?, ?> build) {
        Map<AbstractProject, List<AbstractBuild>> upstreamBuilds = getUpstreamBuildsSinceLastStable(build);
        Map<User, AbstractBuild<?,?>> upstreamCommitters = new HashMap<User, AbstractBuild<?,?>>();
        
        while (upstreamCommitters.isEmpty() && !upstreamBuilds.isEmpty()) {
            Map<AbstractProject, List<AbstractBuild>> currentLevel = upstreamBuilds;
            // new map for the builds one level higher up:
            upstreamBuilds = new HashMap<AbstractProject, List<AbstractBuild>>();
            
            for (Map.Entry<AbstractProject, List<AbstractBuild>> entry : currentLevel.entrySet()) {
                List<AbstractBuild> upstreams = entry.getValue();
                
                for (AbstractBuild upstreamBuild : upstreams) {
                
                    if (upstreamBuild != null) {
                        
                        if (! downstreamIsFirstInRangeTriggeredByUpstream(upstreamBuild, build)) {
                            continue;
                        }
                        
                        Set<User> committers = getCommitters(upstreamBuild);
                        for (User committer : committers) {
                            upstreamCommitters.put(committer, upstreamBuild);
                        }
                        
                        upstreamBuilds.putAll(getUpstreamBuildsSinceLastStable(upstreamBuild));
                    }
                }
            }
        }
        
        return upstreamCommitters;
    }
    
    @SuppressWarnings("rawtypes")
    private Map<AbstractProject, List<AbstractBuild>> getUpstreamBuildsSinceLastStable(AbstractBuild<?,?> currentBuild) {
    	// may be null:
    	AbstractBuild<?, ?> previousSuccessfulBuild = currentBuild.getPreviousSuccessfulBuild();
    	
    	if (previousSuccessfulBuild == null) {
    	    return Collections.emptyMap();
    	}
    	
    	Map<AbstractProject, List<AbstractBuild>> result = new HashMap<AbstractProject, List<AbstractBuild>>();
    	
    	
    	Set<AbstractProject> upstreamProjects = currentBuild.getUpstreamBuilds().keySet();
    	
    	for (AbstractProject upstreamProject : upstreamProjects) {
    	    result.put(upstreamProject, 
    	            getUpstreamBuilds(upstreamProject, previousSuccessfulBuild, currentBuild));
    	}
    	
    	return result;
    }

    /**
     * Gets all upstream builds for a given upstream project and a given downstream since/until build pair
     * 
     * @param upstreamProject the upstream project
     * @param sinceBuild the downstream build since when to get the upstream builds (exclusive)
     * @param untilBuild the downstream build until when to get the upstream builds (inclusive)
     * @return the upstream builds. May be empty but never null
     */
    @SuppressWarnings("rawtypes")
    private List<AbstractBuild> getUpstreamBuilds(
            AbstractProject upstreamProject,
            AbstractBuild<?, ?> sinceBuild,
            AbstractBuild<?, ?> untilBuild) {
        
        List<AbstractBuild> result = Lists.newArrayList();
        
        AbstractBuild<?, ?> sinceBuildUpstreamBuild = sinceBuild.getUpstreamRelationshipBuild(upstreamProject);
        AbstractBuild<?, ?> untilBuildUpstreamBuild = untilBuild.getUpstreamRelationshipBuild(upstreamProject);
        
        AbstractBuild<?, ?> build = sinceBuildUpstreamBuild;
        
        if (build == null) {
        	return result;
        }
        
        do {
            build = build.getNextBuild();
            
            if (build != null) {
                result.add(build);
            }
            
        } while (build != untilBuildUpstreamBuild && build != null);
        
        return result;
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
     * When the completion message is null or empty, no message is sent.
     */
	private void notifyChats(final AbstractBuild<?, ?> build, final BuildListener buildListener) throws IOException, InterruptedException {
        String msg = buildToChatNotifier.buildCompletionMessage(this,build,buildListener);
        if (msg == null || msg.isEmpty()) {
            return;
        }
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
		if (getNotifyOnStart()) {
           if (build.getProject() instanceof MatrixConfiguration) {
               if (getMatrixNotifier() == MatrixJobMultiplier.ONLY_CONFIGURATIONS
                       || getMatrixNotifier() == MatrixJobMultiplier.ALL) {
                   notifyOnBuildStart(build, buildListener);
               }
           } else {
               notifyOnBuildStart(build, buildListener);
           }
		}
		return true;
	}
	
	/**
	 * Sends notification about the build start.
     * When the start message is null or empty, no message is sent.
	 */
	/* package for testing */ void notifyOnBuildStart(AbstractBuild<?, ?> build, BuildListener buildListener) {
	    try {
            final String msg = buildToChatNotifier.buildStartMessage(this,build,buildListener);
            if (msg == null || msg.isEmpty()) {
                return;
            }
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
        } catch (Throwable t) {
            // ignore: never, ever cancel a build because a notification fails
            log(buildListener, "There was an error in the IM plugin: " + ExceptionHelper.dump(t));
        }
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
		    IMMessageTarget imTarget = calculateIMTarget(target, listener);
		    if (imTarget != null) {
		        suspects.add(imTarget);
		    }
		}
		return suspects;
	}

    private IMMessageTarget calculateIMTarget(User target, BuildListener listener) {
        
        String defaultIdSuffix = ((IMPublisherDescriptor)getDescriptor()).getDefaultIdSuffix();
        
        LOGGER.fine("Possible target: " + target.getId());
        String imId = getConfiguredIMId(target);
        if (imId == null && defaultIdSuffix != null) {
            imId = target.getId() + defaultIdSuffix;
        }

        if (imId != null) {
            try {
                return getIMDescriptor().getIMMessageTargetConverter().fromString(imId);
            } catch (final IMMessageTargetConversionException e) {
                log(listener, "Invalid IM ID: " + imId);
            }
        } else {
        	log(listener, "No IM ID found for: " + target.getId());
        }
        
        return null;
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public abstract BuildStepDescriptor<Publisher> getDescriptor();

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
	
    // migrate old instances
    protected Object readResolve() {
    	if (this.strategy == null && this.notificationStrategy != null) {
    		this.strategy = NotificationStrategy.valueOf(this.notificationStrategy.name());
    		this.notificationStrategy = null;
    	}
        if (buildToChatNotifier == null) {
            this.buildToChatNotifier = new DefaultBuildToChatNotifier();
        }
        if (matrixMultiplier == null) {
            this.matrixMultiplier = MatrixJobMultiplier.ONLY_CONFIGURATIONS;
        }
    	return this;
    }
    
    protected final IMPublisherDescriptor getIMDescriptor() {
    	return (IMPublisherDescriptor) getDescriptor();
    }
    
    /**
     * Returns the instant-messaging ID which is configured for a Jenkins user
     * (e.g. via a {@link UserProperty}) or null if there's nothing configured for
     * him/her.
     */
    protected abstract String getConfiguredIMId(User user);
    
    /**
     * Specifies how many notifications to send for matrix projects.
     * Like 'only parent', 'only configurations', 'both'
     */
    public MatrixJobMultiplier getMatrixNotifier() {
        return this.matrixMultiplier;
    }
    
    public void setMatrixNotifier(MatrixJobMultiplier matrixMultiplier) {
        this.matrixMultiplier = matrixMultiplier;
    }
    
    
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {

            @Override
            public boolean startBuild() throws InterruptedException,
                    IOException {
                if (getNotifyOnStart()) {
                    if (getMatrixNotifier() == MatrixJobMultiplier.ALL || getMatrixNotifier() == MatrixJobMultiplier.ONLY_PARENT) {
                        notifyOnBuildStart(build, listener);
                    }
                }
                return super.startBuild();
            }

            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                if (getMatrixNotifier() == MatrixJobMultiplier.ALL || getMatrixNotifier() == MatrixJobMultiplier.ONLY_PARENT) {
                    notifyOnBuildEnd(build, listener);
                }
                return super.endBuild();
            }
        };
    }
    
    // Helper method for the config.jelly
    public boolean isMatrixProject(AbstractProject<?,?> project) {
        return project instanceof MatrixProject;
    }
}
