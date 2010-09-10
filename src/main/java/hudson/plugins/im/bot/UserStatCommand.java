package hudson.plugins.im.bot;

import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.cigame.UserScoreProperty;
import hudson.plugins.im.Sender;
import hudson.tasks.Mailer;

public class UserStatCommand extends AbstractTextSendingCommand {

	@Override
	protected String getReply(Sender sender, String[] args) {
		String userName = args[1];
		User user = User.get(userName, false);
		if (user != null) {
			StringBuilder buf = new StringBuilder();
			buf.append(userName).append(":");
			
			if (!userName.equals(user.getFullName())) {
				buf.append("\n").append("Full name: ").append(user.getFullName());
			}
			
			if (user.getDescription() != null) {
				buf.append("\n").append("Description: ").append(user.getDescription());
			}
			
			Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
			if (mailProperty != null) {
				buf.append("\n").append("E-mail: ").append(mailProperty.getAddress());
			}
			
//			if (Hudson.getInstance().getPlugin("jabber") != null) {
//				
//			}
			
			if (Hudson.getInstance().getPlugin("ci-game") != null) {
				UserScoreProperty property = user.getProperty(UserScoreProperty.class);
				if (property != null) {
					int score = (int) property.getScore();
					buf.append("\n").append("Current score in continuous integration game: ").append(score);
				}
			}
			return buf.toString();
		} else {
			return "Don't know a user named " + userName;
		}
	}

	@Override
	public String getHelp() {
		return " <username> - prints information about a Hudson user";
	}

}
