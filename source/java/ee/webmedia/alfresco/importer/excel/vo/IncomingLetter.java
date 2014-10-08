<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.vo;

import java.util.Date;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public class IncomingLetter extends LetterDocument {
    // docspec:transmittalMode
    private String transmittalMode;
    // docspec:senderName
    private String senderName;
    // docspec:dueDate
    private Date dueDate;

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setTransmittalMode(String sendMode) {
        transmittalMode = sendMode;
    }

    public String getTransmittalMode() {
        return transmittalMode;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

=======
package ee.webmedia.alfresco.importer.excel.vo;

import java.util.Date;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public class IncomingLetter extends LetterDocument {
    // docspec:transmittalMode
    private String transmittalMode;
    // docspec:senderName
    private String senderName;
    // docspec:dueDate
    private Date dueDate;

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setTransmittalMode(String sendMode) {
        transmittalMode = sendMode;
    }

    public String getTransmittalMode() {
        return transmittalMode;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

>>>>>>> develop-5.1
}