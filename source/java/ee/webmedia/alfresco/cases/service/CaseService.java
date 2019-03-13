package ee.webmedia.alfresco.cases.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;

/**
 * Service class for cases
 */
public interface CaseService {
    String BEAN_NAME = "CaseService";
    String NON_TX_BEAN_NAME = "caseService";

    /**
     * Save case using properties not from <code>theCase</code> fields, but from node properties
     *
     * @param theCase
     */
    void saveOrUpdate(Case theCase);

    /**
     * @param theCase
     * @param fromNodeProps - if false properties to be used to update node will be taken from the fields of <code>theCase</code> not from the node properties
     */
    void saveOrUpdate(Case theCase, boolean fromNodeProps);

    void saveAddedAssocs(Node caseNode);

    List<UnmodifiableCase> search(NodeRef volumeRef, String name);

    List<UnmodifiableCase> getAllCasesByVolume(NodeRef volumeRef);

    List<UnmodifiableCase> getAllCasesByVolume(NodeRef volumeRef, DocListUnitStatus status);

    Case getCaseByNoderef(NodeRef caseNodeRef);

    Case getCaseByNoderef(String caseNodeRef);

    Node getCaseNodeByRef(NodeRef caseNodeRef);

    /**
     * @param volumeRef
     * @return Case object with TransientNode and reference to parent Volume
     */
    Case createCase(NodeRef volumeRef);

    /**
     * close given case
     *
     * @param currentCase
     */
    void closeCase(Case currentCase);

    void openCase(Case currentCase);

    /**
     * Close all clases that this volume has
     *
     * @param volumeRef
     */
    void closeAllCasesByVolume(NodeRef volumeRef);

    boolean isClosed(Node node);

    boolean isCaseNameUsed(final String newCaseTitle, NodeRef volumeRef);

    UnmodifiableCase getCaseByTitle(final String newCaseTitle, NodeRef volumeRef, NodeRef caseRef);

    List<NodeRef> getCaseRefsByVolume(NodeRef volumeRef);

    int getCasesCountByVolume(NodeRef volumeRef);

    void delete(Case caseObject);

    String getCaseLabel(NodeRef caseRef);

    void removeFromCache(NodeRef caseRef);

    String getCaseTitle(NodeRef caseRef);
}
