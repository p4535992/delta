package ee.webmedia.alfresco.plumbr;

import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.apache.xpath.operations.String;

public abstract class PlumbrService implements ActivateableBean {

    void setPlumbrActive(Boolean plumbrActive);
    void setPlumbrScriptSrc(String plumbrScriptSrc);
    void setPlumbrAccountId(String plumbrAccountId);
    void setPlumbrAppName(String plumbrAppName);
    void setPlumbrServerUrl(String plumbrServerUrl);


}
