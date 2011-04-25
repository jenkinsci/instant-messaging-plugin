package hudson.plugins.im;

/**
 * Specifies how many notifications should be send for matrix jobs.
 * 
 * @author kutzi
 */
public enum MatrixJobMultiplier {
    ONLY_PARENT("Notify only for parent"),
    ONLY_CONFIGURATIONS("Notify only for configurations"),
    ALL("Notify for parent and all configurations");
    
    private final String description;

    private MatrixJobMultiplier(String description) {
        this.description = description;
    }
    
    public String getLabel() {
        return name().replace('_', ' ');
    }

    public String getDescription() {
        return description;
    }
}
