package hudson.plugins.im;


public class IMPlugin {

	private transient IMConnectionProvider provider;
	private transient JenkinsIsBusyListener busyListener;

    public IMPlugin(IMConnectionProvider provider) {
    	this.provider = provider;
    }
    
    public void start() throws Exception {
    	this.busyListener = JenkinsIsBusyListener.getInstance();
    	this.busyListener.addConnectionProvider(this.provider);
    }

    public void stop() throws Exception {
    	this.busyListener.removeConnectionProvider(this.provider);
    	this.provider.releaseConnection();
    }
}
