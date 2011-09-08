package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;
import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.DOCUMENT_TYPE_ID;
import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.DOCUMENT_TYPE_VERSION_NR;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.CASE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
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
        return getProp(DOCUMENT_TYPE_ID);
    }

    public Integer getDocumentTypeVersionNr() {
        return getProp(DOCUMENT_TYPE_VERSION_NR);
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

    public String getCaseLabelEditable() {
        return getProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
    }

    public void setCaseLabelEditable(String caseLabelEditable) {
        setProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE, caseLabelEditable);
    }

    public boolean isDraft() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME);
    }

    public void setDraft(boolean draft) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME, draft);
    }

}
