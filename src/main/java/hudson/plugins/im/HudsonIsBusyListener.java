package hudson.plugins.im;

import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class HudsonIsBusyListener extends RunListener {
	
	private static final Logger LOGGER = Logger.getLogger(HudsonIsBusyListener.class.getName());
	
	private static HudsonIsBusyListener INSTANCE;
	
	private transient final List<IMConnectionProvider> connectionProviders = new ArrayList<IMConnectionProvider>();
	private transient final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	private transient int lastBusyExecutors = -1;
	private transient int lastTotalExecutors = -1;
	
	private final Runnable updateRunner = new Runnable() {
        @Override
        public void run() {
            updateIMStatus();
        }
    };
    
    public static synchronized HudsonIsBusyListener getInstance() {
    	if (INSTANCE == null) {
    		INSTANCE = new HudsonIsBusyListener();
        	// registration via @Extension didn't seem to work!
        	// Have to retry it sometime.
        	INSTANCE.register();
    	}
    	return INSTANCE;
    }
	
	private HudsonIsBusyListener() {
        super(Run.class);
        this.executor.scheduleAtFixedRate(this.updateRunner, 10, 60, TimeUnit.SECONDS);
        LOGGER.info("Executor busy listener created");
    }
	
	public synchronized void addConnectionProvider(IMConnectionProvider provider) {
		this.connectionProviders.add(provider);
		LOGGER.fine("Added connection provider: " + provider);
	}
	
	public synchronized void removeConnectionProvider(IMConnectionProvider provider) {
		this.connectionProviders.remove(provider);
		LOGGER.fine("Removed connection provider: " + provider);
		
		if (this.connectionProviders.isEmpty()) {
			LOGGER.info("Last connection provider removed. Unregistering this instance.");
			unregister();
			INSTANCE = null;
		}
	}

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        updateLater();
    }
    
	@Override
    public void onDeleted(Run r) {
	    updateLater();
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        updateLater();
    }
    
    private void updateLater() {
        // schedule update 1 second into the future
        // otherwise calculation is often incorrect
        this.executor.schedule(new Runnable() {
            @Override
            public void run() {
                updateIMStatus();
            }
        }, 1L, TimeUnit.SECONDS);
    }
    
    private synchronized void updateIMStatus() {
		int totalExecutors = getTotalExecutors();
        int busyExecutors = getBusyExecutors();
        
        if (totalExecutors != this.lastTotalExecutors || busyExecutors != this.lastBusyExecutors) {
	        for (IMConnectionProvider provider : connectionProviders) {
	        	setStatus(provider, busyExecutors, totalExecutors);
	        }
        }
        this.lastTotalExecutors = totalExecutors;
        this.lastBusyExecutors = busyExecutors;
    }
    
    private void setStatus(IMConnectionProvider provider, int busyExecutors, int totalExecutors) {
    	try {
        	IMConnection conn = provider.currentConnection();
            if (busyExecutors == 0) {
                conn.setPresence(IMPresence.AVAILABLE, "Yawn, I'm so bored. Don't you have some work for me?");
            } else if (busyExecutors == totalExecutors) {
                conn.setPresence(IMPresence.DND, 
                        "Please give me some rest! All " + totalExecutors + " executors are busy, "
                        + Hudson.getInstance().getQueue().getItems().length + " job(s) in queue.");
            } else {
                String msg = "Working: " + busyExecutors + " out of " + totalExecutors +
                    " executors are busy.";
                int queueItems = Hudson.getInstance().getQueue().getItems().length;
                if (queueItems > 0) {
                    msg += " " + queueItems + " job(s) in queue.";
                }
                conn.setPresence(IMPresence.OCCUPIED, msg);
            }
        } catch (IMException e) {
            // ignore
        }
    }
    
    private int getBusyExecutors() {
        int busyExecutors = 0;
        //boolean stillRunningExecutorFound = false;
        Computer[] computers = Hudson.getInstance().getComputers();
        for (Computer compi : computers) {
            
            for (Executor executor : compi.getExecutors()) {
                if (executor.isBusy()) {
                    busyExecutors++;
                }
            }
        }
        
        return busyExecutors;
    }
    
    private int getTotalExecutors() {
        int totalExecutors = 0;
        Computer[] computers = Hudson.getInstance().getComputers();
        for (Computer compi : computers) {
        	if (compi.isOnline()) {
        		totalExecutors += compi.getNumExecutors();
        	}
        }
        return totalExecutors;
    }
}
