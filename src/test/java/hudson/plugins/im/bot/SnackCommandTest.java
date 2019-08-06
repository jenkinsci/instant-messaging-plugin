/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.im.bot;

import hudson.plugins.im.Sender;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        Sender sender = new Sender("tester");
        String[] args = { "!botsnack", "peanuts" };

        String reply = cmd.getReply(null, sender, args);
        System.out.println(reply);
        assertNotNull(reply);
        assertTrue(reply.contains(sender.getNickname()));
        assertTrue(reply.contains("peanuts"));

        args = new String[] { "!botsnack" };
        reply = cmd.getReply(null, sender, args);
        System.out.println(reply);
        assertNotNull(reply);
        assertTrue(reply.contains(sender.getNickname()));
    }
}
