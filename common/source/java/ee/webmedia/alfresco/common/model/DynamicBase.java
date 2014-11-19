package ee.webmedia.alfresco.common.model;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Base type for objects with dynamic configuration
 */
public class DynamicBase extends NodeBaseVO {
    private static final long serialVersionUID = 1L;

    public String getDocumentTypeId() {
        return getProp(Props.OBJECT_TYPE_ID);
    }

    public Integer getDocumentTypeVersionNr() {
        return getProp(Props.OBJECT_TYPE_VERSION_NR);
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

    public boolean isDraft() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME);
    }

    public void setDraft(boolean draft) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT_QNAME, draft);
    }

    public boolean isFromWebService() {
        return getPropBoolean(DocumentService.TransientProps.TEMP_DOCUMENT_IS_FROM_WEB_SERVICE_QNAME);
    }

    public void setFromWebService(boolean fromWebService) {
        setProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_FROM_WEB_SERVICE_QNAME, fromWebService);
    }
}
