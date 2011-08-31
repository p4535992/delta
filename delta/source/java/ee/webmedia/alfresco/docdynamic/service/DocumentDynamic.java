package ee.webmedia.alfresco.docdynamic.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentTemplateService;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

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
        return getProp(DocumentDynamicModel.Props.DOCUMENT_TYPE_ID);
    }

    public Integer getDocumentTypeVersionNr() {
        return getProp(DocumentDynamicModel.Props.DOCUMENT_TYPE_VERSION_NR);
    }

    public String getUrl() {
        return getDocumentTemplateService().getDocumentUrl(getNodeRef());
    }

    @Override
    public DocumentDynamic clone() {
        return (DocumentDynamic) super.clone();
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  node=" + StringUtils.replace(node.toString(), "\n", "\n  ") + "\n]";
    }

    // =========================================================================

    public NodeRef getFunction() {
        return getProp(DocumentDynamicModel.Props.FUNCTION);
    }

    public void setFunction(NodeRef function) {
        setProp(DocumentDynamicModel.Props.FUNCTION, function);
    }

    public NodeRef getSeries() {
        return getProp(DocumentDynamicModel.Props.SERIES);
    }

    public void setSeries(NodeRef series) {
        setProp(DocumentDynamicModel.Props.SERIES, series);
    }

    public NodeRef getVolume() {
        return getProp(DocumentDynamicModel.Props.VOLUME);
    }

    public void setVolume(NodeRef volume) {
        setProp(DocumentDynamicModel.Props.VOLUME, volume);
    }

    public NodeRef getCase() {
        return getProp(DocumentDynamicModel.Props.CASE);
    }

    public void setCase(NodeRef caseRef) {
        setProp(DocumentDynamicModel.Props.CASE, caseRef);
    }

    public String getCaseLabelEditable() {
        return getProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE);
    }

    public void setCaseLabelEditable(String caseLabelEditable) {
        setProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE, caseLabelEditable);
    }

}
