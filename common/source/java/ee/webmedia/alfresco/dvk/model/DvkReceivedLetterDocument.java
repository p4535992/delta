<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
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
=======
package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
