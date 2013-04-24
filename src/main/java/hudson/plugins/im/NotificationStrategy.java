package hudson.plugins.im;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.ResultTrend;
import hudson.plugins.im.tools.BuildHelper;

/**
 * Represents the notification strategy.
 * 
 * @author Uwe Schaefer
 */
public enum NotificationStrategy {

    // Note that the order of the constants also specifies the display order!

	/**
	 * No matter what, notifications should always be send.
	 */
	ALL("all") {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean notificationWanted(final AbstractBuild<?, ?> build) {
			return true;
		}
	},

	/**
	 * Whenever there is a failure, a notification should be send.
	 */
	ANY_FAILURE("failure") {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean notificationWanted(final AbstractBuild<?, ?> build) {
			return build.getResult() != Result.SUCCESS;

		}
	},

	/**
	 * Whenever there is a failure or a failure was fixed, a notification should be send.
	 */
	FAILURE_AND_FIXED("failure and fixed") {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean notificationWanted(final AbstractBuild<?, ?> build) {
            if (build.getResult() != Result.SUCCESS) {
                return true;
            }
            return BuildHelper.isFix(build);
		}
	},

	/**
	 * Whenever there is a new failure or a failure was fixed, a notification should be send.
	 * Similar to #FAILURE_AND_FIXED, but repeated failures do not trigger a notification.
 	 */
	NEW_FAILURE_AND_FIXED("new failure and fixed") {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean notificationWanted(final AbstractBuild<?, ?> build) {
			ResultTrend trend = ResultTrend.getResultTrend(build);
			return trend == ResultTrend.FAILURE || trend == ResultTrend.FIXED;
		}
	},

	/**
	 * Notifications should be send only if there was a change in the build
	 * state, or this was the first build.
	 */
	STATECHANGE_ONLY("change") {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean notificationWanted(final AbstractBuild<?, ?> build) {
			final AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
			return (previousBuild == null)
					|| (build.getResult() != previousBuild.getResult());
		}
	};

	private static final String[] DISPLAY_NAMES;
	
	static {
		DISPLAY_NAMES = new String[NotificationStrategy.values().length];
        int i = 0;
        for (NotificationStrategy strategy : NotificationStrategy.values()) {
        	DISPLAY_NAMES[i++] = strategy.getDisplayName();
        }
	}
	
    private final String displayName;

    private NotificationStrategy(String displayName) {
        this.displayName = displayName;
    }

	/**
	 * Signals if the given build qualifies to send a notification according to
	 * the current strategy.
	 * 
	 * @param build
	 *            The build for which it should be decided, if notification is
	 *            wanted or not.
	 * @return true if, according to the given strategy, a notification should
	 *         be sent.
	 */
	public abstract boolean notificationWanted(AbstractBuild<?, ?> build);

    /**
     * Returns the name of the strategy to display in dialogs etc.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Returns the notification strategy with the given display name.
     *
     * @param displayName the display name
     * @return the notification strategy or null
     */
    public static NotificationStrategy forDisplayName(String displayName) {
        for (NotificationStrategy strategy : values()) {
            if (strategy.getDisplayName().equals(displayName)) {
                return strategy;
            }
        }
        return null;
    }
    
    /**
     * Returns the display names of all notification strategies.
     */
    public static String[] getDisplayNames() {
        return DISPLAY_NAMES;
    }
}
