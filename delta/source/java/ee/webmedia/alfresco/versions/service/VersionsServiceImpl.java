package ee.webmedia.alfresco.versions.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.model.Version;
import ee.webmedia.alfresco.versions.model.VersionsModel;

public class VersionsServiceImpl implements VersionsService {

    private static org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(VersionsServiceImpl.class);

    private VersionServiceExt versionServiceExt;
    private NodeService nodeService;
    private UserService userService;
    private DocumentLogService documentLogService;
    private DictionaryService dictionaryService;

    @Override
    public String getPersonFullNameFromAspect(NodeRef nodeRef, String userName) {
        String fullName = null;
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
            String firstName = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.FIRSTNAME);
            String lastName = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.LASTNAME);
            fullName = UserUtil.getPersonFullName(firstName, lastName);
        }
        if (fullName == null) {
            fullName = userService.getUserFullName(userName);
        }
        return fullName == null ? userName : fullName;
    }

    @Override
    public List<Version> getAllVersions(NodeRef nodeRef, String fileName) {
        List<org.alfresco.service.cmr.version.Version> versionHistory = (List<org.alfresco.service.cmr.version.Version>) versionServiceExt.getVersionHistory(nodeRef)
                .getAllVersions();
        List<Version> list = new ArrayList<Version>(versionHistory.size());
        for (org.alfresco.service.cmr.version.Version v : versionHistory) {
            Version ver = transformVersion(v, fileName);
            list.add(ver);
        }
        return list;
    }

    @Override
    public String calculateNextVersionLabel(NodeRef nodeRef) {
        return versionServiceExt.calculateNextVersionLabel(nodeRef);
    }

    @Override
    public boolean updateVersion(NodeRef nodeRef, String filename, boolean updateOnlyIfNeeded) {
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_LOCKABLE) == true) {
            // if not locked, then a new version can be made
            boolean isLocked = getVersionLockableAspect(nodeRef);
            if (!isLocked) {
                // If the versionable aspect is not there then add it
                if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE) == false) {
                    Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
                    aspectProperties.put(ContentModel.PROP_INITIAL_VERSION, false);
                    aspectProperties.put(ContentModel.PROP_AUTO_VERSION, false);
                    aspectProperties.put(ContentModel.PROP_AUTO_VERSION_PROPS, false);
                    nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, aspectProperties);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Versionable aspect added to " + nodeRef);
                    }
                }

                if (updateOnlyIfNeeded) {
                    org.alfresco.service.cmr.version.Version previousLatestVer = versionServiceExt.getCurrentVersion(nodeRef);
                    if (previousLatestVer != null) {
                        Date frozenModifiedTime = previousLatestVer.getFrozenModifiedDate();
                        String modifier = previousLatestVer.getFrozenModifier();
                        // previousLatestVer.getVersionProperty(name)
                        if (DateUtils.isSameDay(frozenModifiedTime, new Date()) && StringUtils.equals(AuthenticationUtil.getFullyAuthenticatedUser(), modifier)) {
                            logger.info("not creating new version of file with nodeRef=" + nodeRef + " - latest version is modified by same user today");
                            return false;
                        }
                    }
                }

                // create a new version
                versionServiceExt.createVersion(nodeRef, getVersionModifiedAspectProperties(nodeRef));
                // check the flag as true to prevent creation of new versions until the node is unlocked in UnlockMethod
                setVersionLockableAspect(nodeRef, true);
                // log the event
                NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
                if (dictionaryService.isSubClass(nodeService.getType(parentRef), DocumentCommonModel.Types.DOCUMENT)) {
                    String displayName = (String) nodeService.getProperty(nodeRef, FileModel.Props.DISPLAY_NAME);
                    if (StringUtils.isNotBlank(displayName)) {
                        filename = displayName;
                    }
                    documentLogService.addDocumentLog(parentRef //
                            , I18NUtil.getMessage("document_log_status_fileChanged", filename));
                }
                return true;
            }
        }
        logger.warn("not creating new version of nodeRef=" + nodeRef + " - it doesn't have " + VersionsModel.Aspects.VERSION_LOCKABLE.getLocalName() + " aspect");
        return false;
    }

    @Override
    public void updateVersionModifiedAspect(NodeRef nodeRef) {
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED) == true) {
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
            String user = (String) properties.get(ContentModel.PROP_MODIFIER);

            Map<QName, Serializable> personProperties = userService.getUserProperties(user);
            String first = (String) personProperties.get(ContentModel.PROP_FIRSTNAME);
            String last = (String) personProperties.get(ContentModel.PROP_LASTNAME);
            Date modified = DefaultTypeConverter.INSTANCE.convert(Date.class, properties.get(ContentModel.PROP_MODIFIED));

            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, modified);
            aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, first);
            aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, last);

            nodeService.addProperties(nodeRef, aspectProperties);
        }
    }

    @Override
    public boolean getVersionLockableAspect(NodeRef nodeRef) {
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_LOCKABLE) == true) {
            return (Boolean) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionLockable.LOCKED);
        }
        return false;
    }

    @Override
    public void setVersionLockableAspect(NodeRef lockNode, boolean flag) {
        if (nodeService.hasAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE) == true) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting VERSION_LOCKABLE aspect's lock to " + flag + " on nodeRef = " + lockNode);
            }
            nodeService.setProperty(lockNode, VersionsModel.Props.VersionLockable.LOCKED, flag);
        }
    }

    @Override
    public void addVersionLockableAspect(NodeRef lockNode) {
        if (nodeService.hasAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE) == false) {
            nodeService.addAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE, null);
            if (logger.isDebugEnabled()) {
                logger.debug("VERSION_LOCKABLE aspect added to " + lockNode);
            }
        }
    }

    // START: private methods
    private Version transformVersion(org.alfresco.service.cmr.version.Version historicalVersion, String fileName) {
        Version ver = new Version();
        ver.setVersion(historicalVersion.getVersionLabel());
        ver.setDownloadUrl(DownloadContentServlet.generateDownloadURL(historicalVersion.getFrozenStateNodeRef(), fileName));
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

    private Map<String, Serializable> getVersionModifiedAspectProperties(NodeRef nodeRef) {
        Date modified = (Date) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.MODIFIED);
        String first = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.FIRSTNAME);
        String last = (String) nodeService.getProperty(nodeRef, VersionsModel.Props.VersionModified.LASTNAME);

        Map<String, Serializable> props = new HashMap<String, Serializable>(3);
        props.put(VersionsModel.Props.VersionModified.MODIFIED.getLocalName(), modified);
        props.put(VersionsModel.Props.VersionModified.FIRSTNAME.getLocalName(), first);
        props.put(VersionsModel.Props.VersionModified.LASTNAME.getLocalName(), last);

        return props;
    }

    // END: private methods

    // START: getters / setters

    public void setVersionServiceExt(VersionServiceExt versionServiceExt) {
        this.versionServiceExt = versionServiceExt;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    // END: getters / setters
}
