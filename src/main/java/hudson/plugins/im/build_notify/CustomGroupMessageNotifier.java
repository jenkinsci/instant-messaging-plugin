package hudson.plugins.im.build_notify;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.plugins.im.tools.MessageHelper;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @author reynald
 */
public class CustomGroupMessageNotifier extends BuildToChatNotifier {

	private static final String BUILD_NUMBER_TOKEN = "${BUILD_NUMBER}";
	private static final String BUILD_URL_TOKEN = "${BUILD_URL}";

	private final BuildToChatNotifier selectedNotifier;

	public CustomGroupMessageNotifier(final BuildToChatNotifier selectedNotifier) {
		this.selectedNotifier = selectedNotifier;
	}

	@Override
	public String buildStartMessage(final IMPublisher publisher, final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
		String message = publisher.getCustomStartMessage();
		if (StringUtils.isEmpty(message)) {
			message = selectedNotifier.buildStartMessage(publisher, build, listener);
		}

		return replaceTokensInMessage(message, build);
	}

	@Override
	public String buildCompletionMessage(final IMPublisher publisher, final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
		String message;
		if (BuildHelper.isFix(build)) {
			message = publisher.getCustomFixedMessage();
		} else if (build.getResult() == Result.SUCCESS) {
			message = publisher.getCustomSuccessMessage();
		} else if (build.getResult() == Result.UNSTABLE) {
			message = publisher.getCustomUnstableMessage();
		} else if (build.getResult() == Result.FAILURE) {
			message = publisher.getCustomFailedMessage();
		} else {
			message = null;
		}

		if (StringUtils.isEmpty(message)) {
			message = selectedNotifier.buildCompletionMessage(publisher, build, listener);
		}

		return replaceTokensInMessage(message, build);
	}

	private String replaceTokensInMessage(final String message, final AbstractBuild<?, ?> build) {
		// TODO: maybe consider using the token-macro plugin instead
		String replacedMessage = message.replaceAll(BUILD_URL_TOKEN, MessageHelper.getBuildURL(build));
		replacedMessage = replacedMessage.replaceAll(BUILD_NUMBER_TOKEN, String.valueOf(build.getNumber()));

		return replacedMessage;
	}
}
