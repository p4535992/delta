package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
<<<<<<< HEAD
=======
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
<<<<<<< HEAD
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
=======
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
public class DocumentDynamic extends DynamicBase implements Cloneable, Comparable<DocumentDynamic> {
=======
public class DocumentDynamic extends NodeBaseVO implements Cloneable, Comparable<DocumentDynamic> {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private static final long serialVersionUID = 1L;

    protected DocumentDynamic(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

<<<<<<< HEAD
=======
    public String getDocumentTypeId() {
        return getProp(Props.OBJECT_TYPE_ID);
    }

    public Integer getDocumentTypeVersionNr() {
        return getProp(Props.OBJECT_TYPE_VERSION_NR);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public String getUrl() {
        return getDocumentTemplateService().getDocumentUrl(getNodeRef());
    }

    @Override
    public DocumentDynamic clone() {
        try {
            return (DocumentDynamic) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone object: " + toString());
        }
    }

<<<<<<< HEAD
    // =========================================================================

=======
    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  node=" + StringUtils.replace(node.toString(), "\n", "\n  ") + "\n]";
    }

    // =========================================================================

    public NodeRef getFunction() {
        return getProp(FUNCTION);
    }

    public void setFunction(NodeRef function) {
        setProp(FUNCTION, function);
    }

    public NodeRef getSeries() {
        return getProp(SERIES);
    }

    public void setSeries(NodeRef series) {
        setProp(SERIES, series);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public NodeRef getVolume() {
        return getProp(VOLUME);
    }

    public void setVolume(NodeRef volume) {
        setProp(VOLUME, volume);
    }

    public NodeRef getCase() {
        return getProp(CASE);
    }

    public void setCase(NodeRef caseRef) {
        setProp(CASE, caseRef);
    }

    public boolean isDraftOrImapOrDvk() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_OR_IMAP_OR_DVK_QNAME);
    }

    public void setDraftOrImapOrDvk(boolean draftOrImapOrDvk) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_OR_IMAP_OR_DVK_QNAME, draftOrImapOrDvk);
    }

<<<<<<< HEAD
=======
    public boolean isDraft() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME);
    }

    public void setDraft(boolean draft) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME, draft);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public boolean isImapOrDvk() {
        return isDraftOrImapOrDvk() && !isDraft();
    }

    public boolean isIncomingInvoice() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_INCOMING_INVOICE_QNAME);
    }

    public boolean isNotEditable() {
<<<<<<< HEAD
        return getNode().hasAspect(DocumentCommonModel.Aspects.NOT_EDITABLE);
=======
        return getNode().hasAspect(DocumentSpecificModel.Aspects.NOT_EDITABLE); // FIXME: move aspect to DocumentCommonModel
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void setIncomingInvoice(boolean incomingInvoice) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_INCOMING_INVOICE_QNAME, incomingInvoice);
    }

    public boolean isDisableUpdateInitialAccessRestrictionProps() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_DISABLE_UPDATE_INITIAL_ACCESS_RESTRICTION_PROPS);
    }

    public void setDisableUpdateInitialAccessRestrictionProps(boolean disableUpdateInitialAccessRestrictionProps) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_DISABLE_UPDATE_INITIAL_ACCESS_RESTRICTION_PROPS, disableUpdateInitialAccessRestrictionProps);
    }

    public boolean isAccessRestrictionPropsChanged() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_ACCESS_RESTRICTION_PROPS_CHANGED);
    }

    public void setAccessRestrictionPropsChanged(boolean accessRestrictionPropsChanged) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_ACCESS_RESTRICTION_PROPS_CHANGED, accessRestrictionPropsChanged);
    }

    public String getDocName() {
        return getProp(DocumentCommonModel.Props.DOC_NAME);
    }

    public void setDocName(String docName) {
        setProp(DocumentCommonModel.Props.DOC_NAME, docName);
    }

    public String getRegNumber() {
        return getProp(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Date getRegDateTime() {
        return getProp(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getOwnerName() {
        return getProp(DocumentCommonModel.Props.OWNER_NAME);
    }

    @SuppressWarnings("unchecked")
    public String getOwnerOrgStructUnit() {
        return UserUtil.getDisplayUnit((List<String>) getProp(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
    }

    public String getDocStatus() {
        return (String) getProp(DocumentCommonModel.Props.DOC_STATUS);
    }

    public boolean isDocStatus(DocumentStatus status) {
        return status.getValueName().equals(getDocStatus());
    }

    public String getAccessRestriction() {
        return (String) getProp(DocumentCommonModel.Props.ACCESS_RESTRICTION);
    }

    public String getPublishToAdr() {
        return (String) getProp(DocumentDynamicModel.Props.PUBLISH_TO_ADR);
    }

    public String getRecipients() {
        return TextUtil.join(getNode().getProperties(), DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
    }

<<<<<<< HEAD
    public void setDocOldTypeId(String docOldTypeId) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_OLD_TYPE_ID, docOldTypeId);
    }

    public String getDocOldTypeId() {
        return getProp(DocumentService.TransientProps.TEMP_DOCUMENT_OLD_TYPE_ID);
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    @Override
    public int compareTo(DocumentDynamic other) {
        if (StringUtils.equals(getRegNumber(), other.getRegNumber())) {
            if (getRegDateTime() != null) {
                if (other.getRegDateTime() == null) {
                    return 1;
                }
                return getRegDateTime().compareTo(other.getRegDateTime());
            }
            return 0;
        }
        if (getRegNumber() == null) {
            return -1;
        } else if (other.getRegNumber() == null) {
            return 1;
        }
        return AppConstants.DEFAULT_COLLATOR.compare(getRegNumber(), other.getRegNumber());
    }
}
