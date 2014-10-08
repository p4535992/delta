package ee.webmedia.alfresco.document.log.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
<<<<<<< HEAD

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
>>>>>>> develop-5.1
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
<<<<<<< HEAD
import ee.webmedia.alfresco.common.service.GeneralService;
=======
>>>>>>> develop-5.1
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
<<<<<<< HEAD
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
=======
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class DocumentLogServiceImpl implements DocumentLogService {

<<<<<<< HEAD
    private NodeService nodeService;
    private GeneralService generalService;
=======
>>>>>>> develop-5.1
    private UserService userService;
    private LogService logService;

    @Override
    public void addDocumentLog(NodeRef document, String event) {
        String currentUserFullName = userService.getUserFullName();
        addDocumentLog(document, event, currentUserFullName);
    }

    @Override
    public void addDocumentLog(NodeRef document, String event, String creator) {
        addAppLogEntry(document, creator, event);
    }

    @Override
    public void addAssociationLog(NodeRef document, NodeRef targetNodeRef) {
        addAssociationLog(document, targetNodeRef, false);
    }

    @Override
    public void addAssociationLog(NodeRef document, NodeRef targetNodeRef, boolean removed) {
        String action = removed ? "removed" : "add";
<<<<<<< HEAD
        Assert.isTrue(DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(document)));
        QName targetType = nodeService.getType(targetNodeRef);
=======
        Assert.isTrue(DocumentCommonModel.Types.DOCUMENT.equals(getNodeService().getType(document)));
        QName targetType = getNodeService().getType(targetNodeRef);
>>>>>>> develop-5.1
        if (DocumentCommonModel.Types.DOCUMENT.equals(targetType)) {
            addDocumentLog(document, targetNodeRef, "document_log_status_document_assoc_" + action);
            addDocumentLog(targetNodeRef, document, "document_log_status_document_assoc_" + action);
        } else if (CaseModel.Types.CASE.equals(targetType)) {
<<<<<<< HEAD
            addDocumentLog(document, MessageUtil.getMessage("document_log_status_case_assoc_" + action, nodeService.getProperty(targetNodeRef, CaseModel.Props.TITLE)));
        } else if (VolumeModel.Types.VOLUME.equals(targetType)) {
            Map<QName, Serializable> prop = nodeService.getProperties(targetNodeRef);
            addDocumentLog(document,
                        MessageUtil.getMessage("document_log_status_volume_assoc_" + action, prop.get(VolumeModel.Props.VOLUME_MARK), prop.get(VolumeModel.Props.TITLE)));
        } else if (CaseFileModel.Types.CASE_FILE.equals(targetType)) {
            Map<QName, Serializable> prop = nodeService.getProperties(targetNodeRef);
            addDocumentLog(document, MessageUtil.getMessage("document_log_status_casefile_assoc_" + action, prop.get(DocumentDynamicModel.Props.VOLUME_MARK),
                        prop.get(DocumentDynamicModel.Props.TITLE)));
=======
            addDocumentLog(document, MessageUtil.getMessage("document_log_status_case_assoc_" + action, getNodeService().getProperty(targetNodeRef, CaseModel.Props.TITLE)));
        } else if (VolumeModel.Types.VOLUME.equals(targetType)) {
            Map<QName, Serializable> prop = getNodeService().getProperties(targetNodeRef);
            addDocumentLog(document,
                    MessageUtil.getMessage("document_log_status_volume_assoc_" + action, prop.get(VolumeModel.Props.VOLUME_MARK), prop.get(VolumeModel.Props.TITLE)));
        } else if (CaseFileModel.Types.CASE_FILE.equals(targetType)) {
            Map<QName, Serializable> prop = getNodeService().getProperties(targetNodeRef);
            addDocumentLog(document, MessageUtil.getMessage("document_log_status_casefile_assoc_" + action, prop.get(DocumentDynamicModel.Props.VOLUME_MARK),
                    prop.get(DocumentDynamicModel.Props.TITLE)));
>>>>>>> develop-5.1
        }
    }

    private void addDocumentLog(NodeRef document, NodeRef documentRef, String messageid) {
<<<<<<< HEAD
        Map<QName, Serializable> prop = nodeService.getProperties(documentRef);
        addDocumentLog(document, MessageUtil.getMessage(messageid, getDocumentDynamicService().getDocumentTypeName(documentRef),
                        StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.REG_NUMBER)),
                        StringUtils.defaultString(getDate((Date) prop.get(DocumentCommonModel.Props.REG_DATE_TIME))),
                        StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.DOC_NAME))));
=======
        Map<QName, Serializable> prop = getNodeService().getProperties(documentRef);
        addDocumentLog(document, MessageUtil.getMessage(messageid, getDocumentDynamicService().getDocumentTypeName(documentRef),
                StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.REG_NUMBER)),
                StringUtils.defaultString(getDate((Date) prop.get(DocumentCommonModel.Props.REG_DATE_TIME))),
                StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.DOC_NAME))));
>>>>>>> develop-5.1
    }

    private String getDate(Date date) {
        if (date == null) {
            return "";
        }
        return DateFormatUtils.format(date, "dd.MM.yyyy");
    }

    private void addAppLogEntry(NodeRef nodeRef, String creator, String description) {
        String creatorId = userService.getCurrentUserName();
        logService.addLogEntry(LogEntry.createLoc(LogObject.DOCUMENT, creatorId, creator, nodeRef, description));
    }

<<<<<<< HEAD
    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
=======
    @Override
    public void addDeletedObjectLog(NodeRef objectRef, String msgKey) {
        QName objectType = getNodeService().getType(objectRef);
        LogEntry logEntry = LogEntry.create(LogObject.RESTORE, userService, objectRef, msgKey,
                RepoUtil.getArchivedObjectName(objectType, getNodeService().getProperties(objectRef)));
        logEntry.setObjectName(MessageUtil.getTypeName(objectType));
        logService.addLogEntry(logEntry);
    }

    // START: getters / setters
>>>>>>> develop-5.1

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }
    // END: getters / setters
}
