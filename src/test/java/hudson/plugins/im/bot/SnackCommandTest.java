/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.im.bot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.plugins.im.bot.SnackCommand;

import org.junit.Test;

/**
 * Test for the botsnack command.
 * 
 * @author kutzi
 */
public class SnackCommandTest {

    /**
     * Test of executeCommand method, of class SnackCommand.
     */
    @Test
    public void testExecuteCommand() throws Exception {
        SnackCommand cmd = new SnackCommand();
        String sender = "tester";
        String[] args = { "!botsnack", "peanuts" };

        String reply = cmd.getReply(sender, args);
        System.out.println(reply);
        assertNotNull(reply);
        assertTrue(reply.contains(sender));
        assertTrue(reply.contains("peanuts"));
        
        args = new String[] { "!botsnack" };
        reply = cmd.getReply(sender, args);
        System.out.println(reply);
        assertNotNull(reply);
        assertTrue(reply.contains(sender));
    }
}