
package ee.smit.alfresco.plumbr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PlumbrServiceImpl implements PlumbrService {
    private static final Log log = LogFactory.getLog(PlumbrServiceImpl.class);

    private Boolean plumbrActive = false;
    private String plumbrScriptSrc; // = "/scripts/pa.js";
    private String plumbrAccountId;// = "000000000000";
    private String plumbrAppName;// = "UNKNOWN_DELTA";
    private String plumbrServerUrl;// = "https://plumbr.smit.sise";


    /** Getters, Setters */
    public void setPlumbrActive(Boolean plumbrActive){
        logOut("SET", "plumbrActive", String.valueOf(plumbrActive));
        this.plumbrActive = plumbrActive;

    }

    public Boolean isPlumbrActive() {
        logOut("GET", "plumbrActive", String.valueOf(plumbrActive));
        return plumbrActive;
    }

    public void setPlumbrScriptSrc(String plumbrScriptSrc) {
        logOut("SET", "plumbrScriptSrc", plumbrScriptSrc);
        this.plumbrScriptSrc = plumbrScriptSrc;
    }

    public  String getPlumbrScriptSrc() {
        logOut("GET", "plumbrScriptSrc", plumbrScriptSrc);
        return plumbrScriptSrc;
    }

    public void setPlumbrAccountId(String plumbrAccountId) {
        logOut("SET", "plumbrAccountId", plumbrAccountId);
        this.plumbrAccountId = plumbrAccountId;
    }

    public  String getPlumbrAccountId() {
        logOut("GET", "plumbrAccountId", plumbrAccountId);
        return plumbrAccountId;
    }

    public void setPlumbrAppName(String plumbrAppName) {
        logOut("SET", "plumbrAppName", plumbrAppName);
        this.plumbrAppName = plumbrAppName;
    }

    public  String getPlumbrAppName() {
        logOut("GET", "plumbrAppName", plumbrAppName);
        return plumbrAppName;
    }

    public void setPlumbrServerUrl(String plumbrServerUrl) {
        logOut("SET", "plumbrServerUrl", plumbrServerUrl);
        this.plumbrServerUrl = plumbrServerUrl;
    }

    public  String getPlumbrServerUrl() {
        logOut("GET", "plumbrServerUrl", plumbrServerUrl);
        return plumbrServerUrl;
    }

    private void logOut(String type, String name, String value){
        log.debug("-------------------------------------------------");
        log.debug("-- " + type + " " + name + " => " + value);
        log.debug("-------------------------------------------------");
    }
}
