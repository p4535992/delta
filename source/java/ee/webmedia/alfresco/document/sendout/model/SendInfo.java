package ee.webmedia.alfresco.document.sendout.model;

import java.util.Date;

import org.alfresco.web.bean.repository.Node;

public interface SendInfo {

    public static final String SENT = "saadetud";

    public abstract Node getNode();

    public abstract String getRecipient();

    public abstract Date getSendDateTime();

    public abstract String getSendMode();

    public abstract String getSendStatus();

    public abstract String getResolution();

    String getOpenedDateTime();

}