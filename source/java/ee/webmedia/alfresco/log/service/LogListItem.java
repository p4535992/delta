package ee.webmedia.alfresco.log.service;

import java.util.Date;

/**
 * @author Riina Tens
 */
public interface LogListItem {

    Date getCreatedDateTime();

    String getCreatorName();

    String getEventDescription();

}
