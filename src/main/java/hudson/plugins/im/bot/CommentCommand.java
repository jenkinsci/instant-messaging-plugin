package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.im.Sender;
import hudson.plugins.im.tools.MessageHelper;

import java.io.IOException;


public class CommentCommand extends AbstractSingleJobCommand {

    public CommentCommand() {
        super(2);
    }
    
    @Override
    protected CharSequence getMessageForJob(AbstractProject<?, ?> job, Sender sender,
            String[] args) throws CommandException {
        
        try {
            int buildNumber = Integer.parseInt(args[0]);
            Run<?, ?> build = job.getBuildByNumber(buildNumber);
            if (build == null) {
                throw new CommandException("sender: there is no build with number " + args[0] + "!");
            }
            
            build.setDescription(MessageHelper.join(args, 1));
            return "Ok";
        } catch (NumberFormatException e) {
            throw new CommandException("sender: " + args[0] + " is no valid build number!");
        } catch (IOException e) {
            throw new CommandException("Error setting comment: ", e);
        }
    }
    
    @Override
    public String getHelp() {
        return " <job> <build-#> <comment> - adds a description to a build";
    }
}
