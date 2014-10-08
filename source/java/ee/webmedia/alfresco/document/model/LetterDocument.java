<<<<<<< HEAD
package ee.webmedia.alfresco.document.model;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * Parent class for incoming and outgoing letters
 * 
 * @author Ats Uiboupin
 */
@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public abstract class LetterDocument extends ImportDocument {
    private Date senderRegDate;
    private String senderRegNumber;

    public void setSenderRegDate(Date senderRegDate) {
        this.senderRegDate = senderRegDate;
    }

    public Date getSenderRegDate() {
        return senderRegDate;
    }

    public void setSenderRegNumber(String senderRegNumber) {
        this.senderRegNumber = senderRegNumber;
    }

    public String getSenderRegNumber() {
        return senderRegNumber;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
=======
package ee.webmedia.alfresco.document.model;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * Parent class for incoming and outgoing letters
 */
@AlfrescoModelType(uri = DocumentSpecificModel.URI)
public abstract class LetterDocument extends ImportDocument {
    private Date senderRegDate;
    private String senderRegNumber;

    public void setSenderRegDate(Date senderRegDate) {
        this.senderRegDate = senderRegDate;
    }

    public Date getSenderRegDate() {
        return senderRegDate;
    }

    public void setSenderRegNumber(String senderRegNumber) {
        this.senderRegNumber = senderRegNumber;
    }

    public String getSenderRegNumber() {
        return senderRegNumber;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
>>>>>>> develop-5.1
}