package ee.webmedia.alfresco.document.log.service;

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
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.log.model.DocumentLog;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;

public class DocumentLogServiceImpl implements DocumentLogService {

    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;

    @Override
    public List<DocumentLog> getDocumentLogs(NodeRef docRef) {
        return getLogs(docRef, DocumentCommonModel.Assocs.DOCUMENT_LOG);
    }

    @Override
    public List<DocumentLog> getSeriesLogs(NodeRef seriesRef) {
        return getLogs(seriesRef, SeriesModel.Associations.SERIES_LOG);
    }

    @Override
    public void addDocumentLog(NodeRef document, String event) {
        String currentUserFullName = userService.getUserFullName();
        addDocumentLog(document, event, currentUserFullName);
    }

    @Override
    public void addDocumentLog(NodeRef document, String event, String creator) {
        addLogEntry(document, event, creator, DocumentCommonModel.Assocs.DOCUMENT_LOG);
    }

    @Override
    public void addSeriesLog(NodeRef document, String event) {
        String currentUserFullName = userService.getUserFullName();
        addLogEntry(document, event, currentUserFullName, SeriesModel.Associations.SERIES_LOG);
    }

    private void addLogEntry(final NodeRef parentRef, String event, String creator, final QName assocQName) {
        final Map<QName, Serializable> props = new HashMap<QName, Serializable>(3);
        props.put(DocumentCommonModel.Props.CREATED_DATETIME, new Date());
        props.put(DocumentCommonModel.Props.CREATOR_NAME, creator);
        props.put(DocumentCommonModel.Props.EVENT_DESCRIPTION, event);
        // node might be locked by another user - creating node using sys-user
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
            @Override
            public Void doWork() {
                nodeService.createNode(parentRef, assocQName, assocQName,
                        DocumentCommonModel.Types.DOCUMENT_LOG, props);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private List<DocumentLog> getLogs(NodeRef parentRef, QName assocName) {
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, assocName);
        List<DocumentLog> result = new ArrayList<DocumentLog>(assocs.size());
        for (ChildAssociationRef assoc : assocs) {
            result.add(new DocumentLog(generalService.fetchNode(assoc.getChildRef())));
        }
        return result;
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
