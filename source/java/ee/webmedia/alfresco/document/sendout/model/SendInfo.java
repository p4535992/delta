<<<<<<< HEAD
package ee.webmedia.alfresco.document.sendout.model;

import java.util.Date;

import org.alfresco.web.bean.repository.Node;

public interface SendInfo {

    public abstract Node getNode();

    public abstract String getRecipient();

    public abstract Date getSendDateTime();

    public abstract String getSendMode();

    public abstract String getSendStatus();

    public abstract String getResolution();

=======
package ee.webmedia.alfresco.document.sendout.model;

import java.util.Date;

import org.alfresco.web.bean.repository.Node;

public interface SendInfo {

    public abstract Node getNode();

    public abstract String getRecipient();

    public abstract Date getSendDateTime();

    public abstract String getSendMode();

    public abstract String getSendStatus();

    public abstract String getResolution();

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}