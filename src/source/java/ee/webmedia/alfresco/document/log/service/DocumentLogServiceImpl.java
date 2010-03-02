package ee.webmedia.alfresco.document.log.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.log.model.DocumentLog;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

public class DocumentLogServiceImpl implements DocumentLogService {
    
    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;

    @Override
    public List<DocumentLog> getDocumentLogs(NodeRef document) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(document, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.DOCUMENT_LOG);
        List<DocumentLog> result = new ArrayList<DocumentLog>(assocs.size());
        for (ChildAssociationRef assoc : assocs) {
            result.add(new DocumentLog(generalService.fetchNode(assoc.getChildRef())));
        }
        return result;
    }
    
    @Override
    public void addDocumentLog(NodeRef document, String event) {
        String currentUserFullName = userService.getUserFullName();
        addDocumentLog(document, event, currentUserFullName);
    }
    
    @Override
    public void addDocumentLog(NodeRef document, String event, String creator) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3);
        props.put(DocumentCommonModel.Props.CREATED_DATETIME, new Date());
        props.put(DocumentCommonModel.Props.CREATOR_NAME, creator);
        props.put(DocumentCommonModel.Props.EVENT_DESCRIPTION, event);

        nodeService.createNode(document, DocumentCommonModel.Assocs.DOCUMENT_LOG, DocumentCommonModel.Assocs.DOCUMENT_LOG, DocumentCommonModel.Types.DOCUMENT_LOG, props);
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
    // END: getters / setters
}
