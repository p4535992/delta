package ee.webmedia.alfresco.casefile.log.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.PropertyChange;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class CaseFileLogServiceImpl implements CaseFileLogService {

    private NodeService nodeService;
    private UserService userService;
    private LogService logService;

    @Override
    public void addCaseFileLog(NodeRef caseFileRef, String messageid) {
        addCaseFileLog(caseFileRef, MessageUtil.getMessage(messageid), userService.getUserFullName());
    }

    @Override
    public void addCaseFileLogMessage(NodeRef caseFileRef, String message) {
        addCaseFileLog(caseFileRef, message, userService.getUserFullName());
    }

    @Override
    public void addCaseFileLog(NodeRef caseFileRef, String messageid, Object... messageValuesForHolders) {
        addCaseFileLog(caseFileRef, MessageUtil.getMessage(messageid, messageValuesForHolders), userService.getUserFullName());
    }

    @Override
    public void addCaseFileLog(NodeRef caseFileRef, String message, String creator) {
        addAppLogEntry(caseFileRef, creator, message);
    }

    @Override
    public void addCaseFileLog(NodeRef caseFileRef, DocumentDynamic document, String messageid) {
        addCaseFileLog(caseFileRef, messageid, getDocumentDynamicService().getDocumentTypeName(document.getNodeRef()), StringUtils.defaultString(document.getRegNumber()),
                getDate(document.getRegDateTime()),
                document.getDocName());
    }

    private String getDate(Date date) {
        if (date == null) {
            return "";
        }
        return DateFormatUtils.format(date, "dd.MM.yyyy");
    }

    @Override
    public void addCaseFileDocLocChangeLog(PropertyChange propertyChange, DocumentDynamic document) {
        if (propertyChange != null) {
            addCaseFileLog((NodeRef) propertyChange.getOldValue(), document, "casefile_log_document_removed");
            addCaseFileLog((NodeRef) propertyChange.getNewValue(), document, "casefile_log_document");
        }
    }

    @Override
    public void addCaseFileDocLocChangeLog(PropertyChange propertyChange, NodeRef docRef) {
        if (propertyChange != null) {
            addCaseFileLog((NodeRef) propertyChange.getOldValue(), docRef, "casefile_log_document_removed");
            addCaseFileLog((NodeRef) propertyChange.getNewValue(), docRef, "casefile_log_document");
        }
    }

    @Override
    public void addCaseFileLog(NodeRef caseFileRef, NodeRef documentRef, String messageid) {
        Map<QName, Serializable> prop = nodeService.getProperties(documentRef);
        addCaseFileLog(caseFileRef, messageid, getDocumentDynamicService().getDocumentTypeName(documentRef),
                        StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.REG_NUMBER)),
                        StringUtils.defaultString(getDate((Date) prop.get(DocumentCommonModel.Props.REG_DATE_TIME))),
                        StringUtils.defaultString((String) prop.get(DocumentCommonModel.Props.DOC_NAME)));
    }

    @Override
    public void addAssociationLog(NodeRef caseFileRef, NodeRef targetNodeRef) {
        addAssociationLog(caseFileRef, targetNodeRef, false);
    }

    @Override
    public void addAssociationLog(NodeRef caseFileRef, NodeRef targetNodeRef, boolean removed) {
        String action = removed ? "removed" : "add";
        Assert.isTrue(CaseFileModel.Types.CASE_FILE.equals(nodeService.getType(caseFileRef)));
        QName targetType = nodeService.getType(targetNodeRef);
        if (DocumentCommonModel.Types.DOCUMENT.equals(targetType)) {
            addCaseFileLog(caseFileRef, targetNodeRef, "casefile_log_document_assoc_" + action);
        } else if (CaseModel.Types.CASE.equals(targetType)) {
            addCaseFileLog(caseFileRef, "casefile_log_case_assoc_" + action, nodeService.getProperty(targetNodeRef, CaseModel.Props.TITLE));
        } else if (VolumeModel.Types.VOLUME.equals(targetType)) {
            Map<QName, Serializable> prop = nodeService.getProperties(targetNodeRef);
            addCaseFileLog(caseFileRef, "casefile_log_volume_assoc_" + action, prop.get(VolumeModel.Props.VOLUME_MARK), prop.get(VolumeModel.Props.TITLE));
        } else if (CaseFileModel.Types.CASE_FILE.equals(targetType)) {
            Map<QName, Serializable> prop = nodeService.getProperties(targetNodeRef);
            addCaseFileLog(caseFileRef, "casefile_log_casefile_assoc_" + action, prop.get(DocumentDynamicModel.Props.VOLUME_MARK),
                        prop.get(DocumentDynamicModel.Props.TITLE));
            Map<QName, Serializable> propSource = nodeService.getProperties(caseFileRef);
            addCaseFileLog(targetNodeRef, "casefile_log_casefile_assoc_" + action, propSource.get(DocumentDynamicModel.Props.VOLUME_MARK),
                        propSource.get(DocumentDynamicModel.Props.TITLE));
        }
    }

    private void addAppLogEntry(NodeRef nodeRef, String creator, String event) {
        logService.addLogEntry(LogEntry.createLoc(LogObject.CASE_FILE, userService.getCurrentUserName(), creator, nodeRef, event));
    }

    // START: getters / setters

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    // END: getters / setters
}
