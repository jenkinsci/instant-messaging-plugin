package hudson.plugins.im;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract implementation of {@link IMConnection}.
 * All concrete implementations are advised to use this as their base class.
 * 
 * @author kutzi
 */
public abstract class AbstractIMConnection implements IMConnection {

    private final Lock connectionLock = new ReentrantLock();
    
    protected AbstractIMConnection(IMPublisherDescriptor desc) {
    }

    /**
     * Acquires a lock on the underlying connection instance.
     * Should be called before any state changing operations are done on the connection.
     * 
     * Make sure to call {@link #unlock()} in a finally block afterwards!
     */
    protected final void lock() {
        this.connectionLock.lock();
    }
    
    /**
     * Tries to acquire a lock on the underlying connection instance or times out.
     * Should be called before any state changing operations are done on the connection.
     * 
     * Make sure to call {@link #unlock()} in a finally block afterwards!
     */
    protected final boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException {
        return this.connectionLock.tryLock(time, timeUnit);
    }

    /**
     * Releases the lock on the underlying connection instance.
     */
    protected final void unlock() {
        this.connectionLock.unlock();
    }
}
