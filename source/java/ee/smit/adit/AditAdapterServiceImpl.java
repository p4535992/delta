package ee.smit.adit;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class AditAdapterServiceImpl implements AditAdapterService {

    private String defaultUri;
    private boolean aditAdapterActive = false;
    private String regCode;

    // -- GETTERS, SETTERS ------------------------------------------
    public void setDefaultUri(String defaultUri) {
        this.defaultUri = defaultUri;
    }

    public String getUri() {
        return defaultUri;
    }

    public void setAditAdapterActive(boolean aditAdapterActive) {
        this.aditAdapterActive = aditAdapterActive;
    }

    public boolean isAditAdapterActive() {
        return aditAdapterActive;
    }

    public void setRegCode(String regCode) {
        this.regCode = regCode;
    }

    public String getRegCode() {
        return regCode;
    }

}
