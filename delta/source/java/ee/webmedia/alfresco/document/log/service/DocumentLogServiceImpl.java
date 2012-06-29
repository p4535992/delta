package ee.webmedia.alfresco.document.log.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.user.service.UserService;

public class DocumentLogServiceImpl implements DocumentLogService {

    private NodeService nodeService;
    private GeneralService generalService;
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

    private void addAppLogEntry(NodeRef nodeRef, String creator, String description) {
        String creatorId = userService.getCurrentUserName();
        logService.addLogEntry(LogEntry.createLoc(LogObject.DOCUMENT, creatorId, creator, nodeRef, description));
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }
    // END: getters / setters
}
