package ee.webmedia.alfresco.versions.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;

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
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.MapNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.UnableToPerformException;
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
    public List<Version> getAllVersions(NodeRef nodeRef) {
        List<org.alfresco.service.cmr.version.Version> versionHistory = (List<org.alfresco.service.cmr.version.Version>) versionServiceExt.getVersionHistory(nodeRef)
                .getAllVersions();
        List<Version> list = new ArrayList<Version>(versionHistory.size());
        for (org.alfresco.service.cmr.version.Version v : versionHistory) {
            Version ver = transformVersion(v);
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
        Map<String, Serializable> sourceFileProp = getVersionModifiedAspectProperties(nodeRef);
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_LOCKABLE)) {
            // if not locked, then a new version can be made
            boolean isLocked = getVersionLockableAspect(nodeRef);
            if (!isLocked) {
                // If the versionable aspect is not there then add it
                addVersionableAspect(nodeRef);

                if (updateOnlyIfNeeded && !Boolean.TRUE.equals(nodeService.getProperty(nodeRef, FileModel.Props.NEW_VERSION_ON_NEXT_SAVE))) {
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
                org.alfresco.service.cmr.version.Version version = versionServiceExt.createVersion(nodeRef, sourceFileProp);
                logger.info("Created new version (" + version.getVersionLabel() + ") from " + nodeRef + " ( " + filename + "). VersionedNodeRef: " + version.getVersionedNodeRef()
                        + " FrozenStateNodeRef: " + version.getFrozenStateNodeRef());
                // check the flag as true to prevent creation of new versions until the node is unlocked in UnlockMethod
                setVersionLockableAspect(nodeRef, true);

                nodeService.removeProperty(nodeRef, FileModel.Props.COMMENT);

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
            logger.warn("not creating new version of nodeRef=" + nodeRef + " - it is locked!");
        } else {
            logger.warn("not creating new version of nodeRef=" + nodeRef + " - it doesn't have " + VersionsModel.Aspects.VERSION_LOCKABLE.getLocalName() + " aspect!");
        }
        return false;
    }

    @Override
    public void activateVersion(NodeRef versionRef) {
        Map<QName, Serializable> versionProp = nodeService.getProperties(versionRef);
        // should take sourceRef value from frozenNodeRef (Version2Model.PROP_QNAME_FROZEN_NODE_REF)
        NodeRef sourceRef = new NodeRef((String) versionProp.get(ContentModel.PROP_STORE_PROTOCOL), (String) versionProp.get(ContentModel.PROP_STORE_IDENTIFIER),
                (String) versionProp.get(ContentModel.PROP_NODE_UUID));
        if (getDocLockService().getLockStatus(sourceRef) == LockStatus.LOCKED) {
            throw new NodeLockedException(sourceRef);
        }
        Map<QName, Serializable> sourceProp = nodeService.getProperties(sourceRef);
        if (!updateVersion(sourceRef, (String) sourceProp.get(ContentModel.PROP_NAME), false)) {
            throw new UnableToPerformException("version_activate_error");
        }
        updateVersionModifiedAspect(sourceRef);
        nodeService.setProperty(sourceRef, ContentModel.PROP_CONTENT, versionProp.get(ContentModel.PROP_CONTENT));
        setVersionLockableAspect(sourceRef, false);
    }

    @Override
    public void updateVersionModifiedAspect(NodeRef nodeRef) {
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
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
        if (nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_LOCKABLE)) {
            // If the property isn't set, then this version isn't locked
            return Boolean.TRUE.equals(nodeService.getProperty(nodeRef, VersionsModel.Props.VersionLockable.LOCKED));
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
    public boolean addVersionLockableAspect(NodeRef lockNode) {
        boolean aspectAdded = false;
        if (!nodeService.hasAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE)) {
            nodeService.addAspect(lockNode, VersionsModel.Aspects.VERSION_LOCKABLE, null);
            if (logger.isDebugEnabled()) {
                logger.debug("VERSION_LOCKABLE aspect added to " + lockNode);
            }
            aspectAdded = true;
        }

        return aspectAdded;
    }

    // START: private methods
    @Override
    public boolean addVersionableAspect(NodeRef nodeRef) {
        boolean aspectAdded = false;
        if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
            logger.info("Adding " + ContentModel.ASPECT_VERSIONABLE.getLocalName() + " to " + nodeRef);
            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(ContentModel.PROP_INITIAL_VERSION, false);
            aspectProperties.put(ContentModel.PROP_AUTO_VERSION, false);
            aspectProperties.put(ContentModel.PROP_AUTO_VERSION_PROPS, false);
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, aspectProperties);
            if (logger.isDebugEnabled()) {
                logger.debug("Versionable aspect added to " + nodeRef);
            }
            aspectAdded = true;
        }

        return aspectAdded;
    }

    private Version transformVersion(org.alfresco.service.cmr.version.Version historicalVersion) {
        String fileName = (String) historicalVersion.getVersionProperty(ContentModel.PROP_NAME.getLocalName());
        Version ver = new Version();
        ver.setVersion(historicalVersion.getVersionLabel());
        ver.setDownloadUrl(DownloadContentServlet.generateDownloadURL(historicalVersion.getFrozenStateNodeRef(), fileName));
        ver.setModified(getModifiedDateFromAspect(historicalVersion));
        ver.setAuthor(getPersonFullNameFromAspect(historicalVersion.getFrozenStateNodeRef(), historicalVersion.getFrozenModifier()));
        ver.setComment((String) historicalVersion.getVersionProperties().get(VersionsModel.Props.VersionModified.COMMENT.getLocalName()));
        ver.setNodeRef(historicalVersion.getFrozenStateNodeRef());
        ver.setNode(new MapNode(historicalVersion.getVersionedNodeRef()));
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

    @Override
    public void addVersionModifiedAspect(NodeRef nodeRef) {
        if (!nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

            String user = (String) properties.get(ContentModel.PROP_CREATOR);

            Date modified = DefaultTypeConverter.INSTANCE.convert(Date.class, properties.get(ContentModel.PROP_CREATED));
            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, modified);

            Map<QName, Serializable> personProps = userService.getUserProperties(user);
            String first = (String) personProps.get(ContentModel.PROP_FIRSTNAME);
            String last = (String) personProps.get(ContentModel.PROP_LASTNAME);

            aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, first);
            aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, last);

            nodeService.addAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED, aspectProperties);
        }
    }

    private Map<String, Serializable> getVersionModifiedAspectProperties(NodeRef nodeRef) {
        Map<QName, Serializable> fileProps = nodeService.getProperties(nodeRef);
        Map<String, Serializable> props = new HashMap<String, Serializable>(4);
        props.put(VersionsModel.Props.VersionModified.MODIFIED.getLocalName(), fileProps.get(VersionsModel.Props.VersionModified.MODIFIED));
        props.put(VersionsModel.Props.VersionModified.FIRSTNAME.getLocalName(), fileProps.get(VersionsModel.Props.VersionModified.FIRSTNAME));
        props.put(VersionsModel.Props.VersionModified.LASTNAME.getLocalName(), fileProps.get(VersionsModel.Props.VersionModified.LASTNAME));
        props.put(VersionsModel.Props.VersionModified.COMMENT.getLocalName(), fileProps.get(FileModel.Props.COMMENT));
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
