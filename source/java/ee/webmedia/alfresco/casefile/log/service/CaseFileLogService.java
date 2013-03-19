package ee.webmedia.alfresco.casefile.log.service;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.PropertyChange;

/**
 * @author Priit Pikk
 */
public interface CaseFileLogService {

    String BEAN_NAME = "CaseFileLogService";

    void addCaseFileLog(NodeRef caseFileRef, String messageid);

    public void addCaseFileLogMessage(NodeRef caseFileRef, String message);

    void addCaseFileLog(NodeRef caseFileRef, String messageid, Object... messageValuesForHolders);

    void addCaseFileLog(NodeRef caseFileRef, String message, String creator);

    void addCaseFileLog(NodeRef caseFileRef, DocumentDynamic document, String event);

    void addCaseFileLog(NodeRef caseFileRef, NodeRef documentRef, String event);

    void addCaseFileDocLocChangeLog(PropertyChange propertyChange, DocumentDynamic document);

    void addCaseFileDocLocChangeLog(PropertyChange propertyChange, NodeRef docRef);

    void addAssociationLog(NodeRef caseFileRef, NodeRef targetNodeRef);

    void addAssociationLog(NodeRef caseFileRef, NodeRef targetNodeRef, boolean removed);
}
