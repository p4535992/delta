package ee.webmedia.alfresco.versions.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.web.app.servlet.DownloadContentServlet;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.versions.model.Version;
import ee.webmedia.alfresco.versions.model.VersionsModel;

public class VersionsServiceImpl implements VersionsService {

    private VersionService versionService;
    private NodeService nodeService;
    private GeneralService generalService;

    @Override
    public String getPersonFullNameFromAspect(NodeRef nodeRef, String userName) {
        String fullName = null;
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
            String first = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.FIRSTNAME);
            String last = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.LASTNAME);
            fullName = generalService.getFirstAndLastNames(first, last, " ");
        }
        if (fullName == null) {
            fullName = generalService.getPersonFullNameByUserName(userName);
        }
        return fullName == null ? userName : fullName;
    }

    @Override
    public List<Version> getAllVersions(NodeRef nodeRef, String fileName) {
        List<Version> list = new ArrayList<Version>();
        @SuppressWarnings("unchecked")
        List<org.alfresco.service.cmr.version.Version> versionHistory = (List) versionService.getVersionHistory(nodeRef).getAllVersions();
        for (int i = 0; i < versionHistory.size(); i++) {
            Version ver = transformVersion(versionHistory.get(i), fileName);
            list.add(ver);
        }
        return list;
    }

    // START: private methods
    private Version transformVersion(org.alfresco.service.cmr.version.Version historicalVersion, String fileName) {
        Version ver = new Version();
        ver.setVersion(historicalVersion.getVersionLabel());
        if (historicalVersion.getFrozenStateNodeRef() != null && fileName != null) {
            ver.setDownloadUrl(DownloadContentServlet.generateDownloadURL(historicalVersion.getFrozenStateNodeRef(), fileName));
        }
        ver.setModified(getModifiedDateFromAspect(historicalVersion));
        ver.setAuthor(getPersonFullNameFromAspect(historicalVersion.getFrozenStateNodeRef(), historicalVersion.getFrozenModifier()));
        return ver;
    }

    private Date getModifiedDateFromAspect(org.alfresco.service.cmr.version.Version historicalVersion) {
        if (nodeService.hasAspect(historicalVersion.getFrozenStateNodeRef(), VersionsModel.Aspects.VERSION_MODIFIED)) {
            Date date = DefaultTypeConverter.INSTANCE.convert(Date.class, historicalVersion.getVersionProperty(VersionsModel.Props.VersionModified.MODIFIED
                    .getLocalName()));
            return date;
        }
        return historicalVersion.getFrozenModifiedDate();
    }

    // END: private methods

    // START: getters / setters

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    // END: getters / setters
}
