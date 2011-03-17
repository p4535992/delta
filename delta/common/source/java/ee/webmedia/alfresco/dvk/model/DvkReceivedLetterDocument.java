package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 *
 */
@AlfrescoModelType(uri = DvkModel.URI)
public interface DvkReceivedLetterDocument extends ILetterDocument {


    /**
     * @return dhl_id - unique id assigned to the sent document by the DVK server
     */
    String getDvkId();

    void setDvkId(String dvkId);

    Date getLetterDeadLine();

    void setLetterDeadLine(Date letterDeadLine);

}
