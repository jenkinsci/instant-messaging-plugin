package hudson.plugins.im;

import org.acegisecurity.Authentication;

/**
 * Just a holder to return a (possibly cached) {@link Authentication}.
 *
 * @author kutzi
 */
public interface AuthenticationHolder {
    /**
     * Return the {@link Authentication}.
     */
    Authentication getAuthentication();
}
