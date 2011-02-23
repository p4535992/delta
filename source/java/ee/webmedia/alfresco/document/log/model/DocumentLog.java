package ee.webmedia.alfresco.document.log.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date createdDateTime;
    private String creatorName;
    private String eventDescription;

    public DocumentLog() {
    }

    public DocumentLog(Node node) {
        createdDateTime = (Date) node.getProperties().get(DocumentCommonModel.Props.CREATED_DATETIME);
        creatorName = (String) node.getProperties().get(DocumentCommonModel.Props.CREATOR_NAME);
        eventDescription = (String) node.getProperties().get(DocumentCommonModel.Props.EVENT_DESCRIPTION);
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getEventDescription() {
        return eventDescription;
    }
}
