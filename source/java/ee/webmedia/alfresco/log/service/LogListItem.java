package ee.webmedia.alfresco.log.service;

import java.util.Date;

public interface LogListItem {

    Date getCreatedDateTime();

    String getCreatorName();

    String getEventDescription();

}
