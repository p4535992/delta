package ee.webmedia.alfresco.document.permissions;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class DocumentListCreateDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentListCreateDynamicAuthority.class);

    public static final String DOCUMENT_LIST_CREATE_AUTHORITY = "ROLE_DOCUMENT_LIST_CREATE";

    public static Set<QName> types = new HashSet<QName>();

    public DocumentListCreateDynamicAuthority() {
        types.add(FunctionsModel.Types.FUNCTIONS_ROOT);
        types.add(FunctionsModel.Types.FUNCTION);
        types.add(SeriesModel.Types.SERIES);
    }

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (type.equals(DocumentCommonModel.Types.DRAFTS) || type.equals(VolumeModel.Types.VOLUME)) {
            log.debug("Anyone can create a node under drafts or volume, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        if (dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.debug("Node is a document, granting authority " + getAuthority());
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
        return DOCUMENT_LIST_CREATE_AUTHORITY;
    }

}
