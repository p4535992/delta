package ee.webmedia.alfresco.casefile.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

public interface CaseFileService {

    public static final String BEAN_NAME = "CaseFileService";

    /**
     * Create a new case file and set default property values according to fully authenticated user.
     * 
     * @param typeId
     * @param parent
     * @return
     */
    Pair<CaseFile, DocumentTypeVersion> createNewCaseFile(String typeId, NodeRef parent);

    Pair<CaseFile, DocumentTypeVersion> createNewCaseFile(DocumentTypeVersion docVer, NodeRef parent, boolean reallySetDefaultValues);

    /**
     * Create a new case file and set default property values according to fully authenticated user.
     * 
     * @param typeId
     * @param parent
     * @return
     */
    Pair<CaseFile, DocumentTypeVersion> createNewCaseFileInDrafts(String typeId);

    CaseFile getCaseFile(NodeRef caseFileRef);

    CaseFile update(CaseFile caseFile, List<String> saveListenerBeanNames);

    List<DocumentToCompoundWorkflow> getCaseFileDocumentWorkflows(NodeRef caseFileRef);

    void registerCaseFile(Node caseFile, Node previousSeries, boolean relocating);

    Node getSeriesByCaseFile(NodeRef caseFileNodeRef);

    void closeCaseFile(CaseFile caseFile);

    void openCaseFile(CaseFile caseFile);

    void deleteCaseFile(NodeRef nodeRef, String reason);

}
