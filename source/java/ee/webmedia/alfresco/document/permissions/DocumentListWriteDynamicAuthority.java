package ee.webmedia.alfresco.document.permissions;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class DocumentListWriteDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentListWriteDynamicAuthority.class);

    public static final String DOCUMENT_LIST_WRITE_AUTHORITY = "ROLE_DOCUMENT_LIST_WRITE";

    public static Set<QName> types = new HashSet<QName>();

    public DocumentListWriteDynamicAuthority() {
        types.add(FunctionsModel.Types.FUNCTION);
        types.add(SeriesModel.Types.SERIES);
        types.add(VolumeModel.Types.VOLUME);
        types.add(CaseModel.Types.CASE);
    }

    @Override
    public boolean hasAuthority(NodeRef nodeRef, String userName) {
        QName type = nodeService.getType(nodeRef);
        if (type.equals(DocumentCommonModel.Types.DRAFTS) || type.equals(VolumeModel.Types.VOLUME)) {
            log.debug("Anyone can create a node under drafts or volume, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        if (!types.contains(type)) {
            log.trace("Node is not of a type belonging to document list, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a documentManager on node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        log.trace("No conditions met on node, type=" + type + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_LIST_WRITE_AUTHORITY;
    }

}
