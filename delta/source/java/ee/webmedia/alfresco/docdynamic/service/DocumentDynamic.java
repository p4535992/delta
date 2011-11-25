package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * @author Alar Kvell
 */
public class DocumentDynamic extends NodeBaseVO implements Cloneable {
    private static final long serialVersionUID = 1L;

    protected DocumentDynamic(WmNode node) {
        Assert.notNull(node);
        this.node = node;
    }

    public String getDocumentTypeId() {
        return getProp(Props.OBJECT_TYPE_ID);
    }

    public Integer getDocumentTypeVersionNr() {
        return getProp(Props.OBJECT_TYPE_VERSION_NR);
    }

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

    public boolean isDraft() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME);
    }

    public void setDraft(boolean draft) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME, draft);
    }

    public boolean isImapOrDvk() {
        return isDraftOrImapOrDvk() && !isDraft();
    }

    public boolean isIncomingInvoice() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_INCOMING_INVOICE_QNAME);
    }

    public boolean isNotEditable() {
        return getNode().hasAspect(DocumentSpecificModel.Aspects.NOT_EDITABLE); // FIXME: move aspect to DocumentCommonModel
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

    public String getRegNumber() {
        return getProp(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Date getRegDateTime() {
        return getProp(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getOwnerName() {
        return getProp(DocumentCommonModel.Props.OWNER_NAME);
    }
}
