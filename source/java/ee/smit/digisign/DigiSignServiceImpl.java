package ee.smit.digisign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DigiSignServiceImpl implements DigiSignService {
    protected final Log log = LogFactory.getLog(getClass());

    private String appname;
    private String apppass;
    private String defaultUri;
    private boolean active;
    private int batchSize;

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    private int maxThreads = 0;

    // Getters, Setters ------------------------
    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getAppname() {
        return appname;
    }

    public void setApppass(String apppass) {
        this.apppass = apppass;
    }

    public String getApppass() {
        return apppass;
    }

    public void setDefaultUri(String defaultUri) {
        this.defaultUri = defaultUri;
    }

    public String getUri() {
        return defaultUri;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getActive() {
        return active;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
