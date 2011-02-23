package ee.webmedia.alfresco.dvk.model;

import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 *
 */
// use URIs defined by the interfaces
@AlfrescoModelType(uri = "")
public class DvkReceivedDocumentImpl extends AbstractDocument implements DvkReceivedDocument {

    /**
     * aka dhl_id - unique id assigned to the sent document by the DVK server
     */
    private String dvkId;
    private Date letterDeadLine;


    @Override
    public String getDvkId() {
        return dvkId;
    }

    @Override
    public void setDvkId(String dvkId) {
        this.dvkId = dvkId;
    }


    @Override
    public Date getLetterDeadLine() {
        return letterDeadLine;
    }

    @Override
    public void setLetterDeadLine(Date letterDeadLine) {
        this.letterDeadLine = letterDeadLine;
    }

}
