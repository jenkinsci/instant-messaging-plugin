package hudson.plugins.im;

import hudson.model.Cause;

/**
 * Marks a build that was started because of an action triggered
 * in a IMPlugin.
 *
 * @author kutzi
 */
public class IMCause extends Cause {

	private final String description;

	public IMCause(String description) {
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getShortDescription() {
		return this.description;
	}
}
