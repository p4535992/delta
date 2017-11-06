package ee.webmedia.alfresco.plumbr;

public class PlumbrServiceImpl extends PlumbrService {

    private boolean active = true;

    private Boolean plumbrActive;
    private String plumbrScriptSrc;
    private String plumbrAccountId;
    private String plumbrAppName;
    private String plumbrServerUrl;

    public void setPlumbrActive(Boolean plumbrActive){
        this.plumbrActive = plumbrActive;
    }

}
