package hudson.plugins.im;


public class IMPlugin {

	private transient IMConnectionProvider provider;
	private transient HudsonIsBusyListener busyListener;

    public IMPlugin(IMConnectionProvider provider) {
    	this.provider = provider;
    }
    
    public void start() throws Exception {
    	this.busyListener = new HudsonIsBusyListener();
    	// registration via @Extension doesn't seem to work!?
    	this.busyListener.register();
    	this.busyListener.addConnectionProvider(this.provider);
    }

    public void stop() throws Exception {
    	this.busyListener.unregister();
    	this.provider.releaseConnection();
    }
}
