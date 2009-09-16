package hudson.plugins.im;

import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class HudsonIsBusyListener extends RunListener {
	
	private static final Logger LOGGER = Logger.getLogger(HudsonIsBusyListener.class.getName());
	private transient final List<IMConnectionProvider> connectionProviders = new ArrayList<IMConnectionProvider>();
	
	public HudsonIsBusyListener() {
        super(Run.class);
        LOGGER.info("Executor busy listener created");
    }
	
	public void addConnectionProvider(IMConnectionProvider provider) {
		this.connectionProviders.add(provider);
		LOGGER.fine("Added connection provider: " + provider);

		// update status with some delay as it usually takes some time
		// until the connection is established
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1500L);
				} catch (InterruptedException e) {
					// ignore
				}
				updateIMStatus();
			}
		}.start();
	}
	
	public void removeConnectionProvider(IMConnectionProvider provider) {
		this.connectionProviders.remove(provider);
		LOGGER.fine("Removed connection provider: " + provider);
	}

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        // the executor of 'r' is still busy, we have to take that into account!
        updateIMStatus(r);
    }
    
	@Override
	public void onFinalized(Run r) {
		updateIMStatus(r);
	}

	@Override
    public void onDeleted(Run r) {
        updateIMStatus(null);
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        updateIMStatus(null);
    }
    
    protected void updateIMStatus() {
        updateIMStatus(null);
    }
    
    private void updateIMStatus(Run<?, ?> run) {
        int totalExecutors = getTotalExecutors();
        int busyExecutors = getBusyExecutors(run);
        
        for (IMConnectionProvider provider : this.connectionProviders) {
        	setStatus(provider, busyExecutors, totalExecutors);
        }
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
    
    private int getBusyExecutors(Run<?, ?> run) {
        int busyExecutors = 0;
        boolean stillRunningExecutorFound = false;
        Computer[] computers = Hudson.getInstance().getComputers();
        for (Computer compi : computers) {
            
            for (Executor executor : compi.getExecutors()) {
                if (executor.isBusy()) {
                    if (isNotEqual(executor.getCurrentExecutable(), run)) {
                        busyExecutors++;
                    } else {
                    	stillRunningExecutorFound = true;
                    }
                }
            }
        }
        
        if ( run != null && !stillRunningExecutorFound) {
        	LOGGER.warning("Didn't find executor for run " + run + " among the list of busy executors.");
        	// Decrease anyway.
        	// Otherwise count would be wrong. See [HUDSON-4337]
        	// Don't know why the detection doesn't work reliably
        	if (busyExecutors > 0) {
        		busyExecutors--;
        	}
        }
        
        return busyExecutors;
    }
    
    private int getTotalExecutors() {
        int totalExecutors = 0;
        Computer[] computers = Hudson.getInstance().getComputers();
        for (Computer compi : computers) {
            totalExecutors += compi.getNumExecutors();
        }
        return totalExecutors;
    }
        
    private static boolean isNotEqual(Queue.Executable executable, Run<?, ?> run) {
        if (run == null) {
            return true;
        }
        
        if (executable instanceof Run<?, ?>) {
        	return !((Run<?, ?>)executable).getId().equals(run.getId());
        } else {
        	// can never be equal
        	return false;
        }
    }

}
