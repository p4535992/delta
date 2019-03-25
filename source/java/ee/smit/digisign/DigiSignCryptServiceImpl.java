package ee.smit.digisign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DigiSignCryptServiceImpl implements DigiSignCryptService {
    protected final Log log = LogFactory.getLog(getClass());

    private String appname;
    private String apppass;
    private String defaultUri;
    private boolean active;

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

    public boolean getActive() {
        return active;
    }

    public void setDefaultUri(String defaultUri) {
        this.defaultUri = defaultUri;
    }

    public String getUri() {
        return defaultUri;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
