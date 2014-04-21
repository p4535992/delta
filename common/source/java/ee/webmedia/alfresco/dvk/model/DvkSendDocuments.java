package ee.webmedia.alfresco.dvk.model;

import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

public class DvkSendDocuments extends AbstractDvkDocument {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkSendDocuments.class);

    private List<String> recipientsRegNrs;
    private List<String> orgNames;
    private List<String> personNames;
    private List<String> personIdCodes;
    private NodeRef documentNodeRef;

    public void validateOutGoing() {
        try {
            Assert.assertNotNull("dvk_send_error_missingData_senderEmail_null", getSenderEmail());
            List<String> recipientsRegNrs = getRecipientsRegNrs();
            List<String> personIdCodes = getPersonIdCodes();
            Assert.assertFalse("dvk_send_error_missingData_recipientsRegNrs_null", recipientsRegNrs == null && personIdCodes == null);
            Assert.assertFalse("dvk_send_error_missingData_recipients_notAdded", CollectionUtils.isEmpty(recipientsRegNrs) && CollectionUtils.isEmpty(personIdCodes));
        } catch (AssertionFailedError e) {
            log.debug("Object that was validated: '" + ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE) + "'");
            final UnableToPerformException unableToPerformException = new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_missingData", e);
            unableToPerformException.setMessageValuesForHolders(new MessageDataImpl(e.getMessage()));
            throw unableToPerformException;
        }
    }

    public List<String> getRecipientsRegNrs() {
        return recipientsRegNrs;
    }

    public void setRecipientsRegNrs(List<String> recipientsRegNrs) {
        this.recipientsRegNrs = recipientsRegNrs;
    }

    public List<String> getOrgNames() {
        return orgNames;
    }

    public void setOrgNames(List<String> orgNames) {
        this.orgNames = orgNames;
    }

    public List<String> getPersonNames() {
        return personNames;
    }

    public void setPersonNames(List<String> personNames) {
        this.personNames = personNames;
    }

    public List<String> getPersonIdCodes() {
        return personIdCodes;
    }

    public void setPersonIdCodes(List<String> personIdCodes) {
        this.personIdCodes = personIdCodes;
    }

    public NodeRef getDocumentNodeRef() {
        return documentNodeRef;
    }

    public void setDocumentNodeRef(NodeRef documentNodeRef) {
        this.documentNodeRef = documentNodeRef;
    }

}
