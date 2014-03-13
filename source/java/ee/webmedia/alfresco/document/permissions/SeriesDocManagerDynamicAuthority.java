package ee.webmedia.alfresco.document.permissions;

import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * Evaluates if current runAsUser is member of documentManagers group and also has permission set for series of the document.
 */
public class SeriesDocManagerDynamicAuthority extends BaseDynamicAuthority {
    public static final String SERIES_MANAGEABLE_PERMISSION = "DocumentFileRead";
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesDocManagerDynamicAuthority.class);
    public static final String ROLE_SERIES_DOCUMENT_MANAGERS = "ROLE_SERIES_DOCUMENT_MANAGERS";
    private GeneralService generalService;

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        if (!isDocManager()) {
            log.debug("User " + userName + " is not a document manager on node, type=" + type + ", not granting authority " + getAuthority());
            return false;
        }
        NodeRef seriesRef = generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES);
        if (AccessStatus.ALLOWED == permissionServiceImpl.hasPermission(seriesRef, SERIES_MANAGEABLE_PERMISSION)) {
            log.debug("User " + userName + " is a document manager of the series of document " + nodeRef + ", type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        return false;
    }

    private boolean isDocManager() {
        final Set<String> authorities = authorityService.getAuthorities();
        return authorities.contains(documentManagersGroup);
    }

    @Override
    public String getAuthority() {
        return ROLE_SERIES_DOCUMENT_MANAGERS;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
