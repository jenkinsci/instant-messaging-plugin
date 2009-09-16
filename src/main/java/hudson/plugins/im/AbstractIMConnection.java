package hudson.plugins.im;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractIMConnection implements IMConnection {

    private final Lock connectionLock = new ReentrantLock();
    
    protected AbstractIMConnection(IMPublisherDescriptor desc) {
    }
    
    protected final void lock() {
        this.connectionLock.lock();
    }
    
    protected final boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException {
        return this.connectionLock.tryLock(time, timeUnit);
    }

    protected final void unlock() {
        this.connectionLock.unlock();
    }
}
