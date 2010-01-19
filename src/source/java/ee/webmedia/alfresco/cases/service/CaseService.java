package ee.webmedia.alfresco.cases.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.cases.model.Case;

/**
 * Service class for cases
 * 
 * @author Ats Uiboupin
 */
public interface CaseService {
    String BEAN_NAME = "CaseService";

    /**
     * Save case using properties not from <code>theCase</code> fields, but from node properties
     * @param theCase
     */
    void saveOrUpdate(Case theCase);
    
    /**
     * @param theCase
     * @param fromNodeProps - if false properties to be used to update node will be taken from the fields of <code>theCase</code> not from the node properties
     */
    void saveOrUpdate(Case theCase, boolean fromNodeProps);

    List<Case> getAllCasesByVolume(NodeRef volumeRef);

    Case getCaseByNoderef(String caseNodeRef);

    Node getCaseNodeByRef(NodeRef caseNodeRef);

    /**
     * @param volumeRef
     * @return Case object with TransientNode and reference to parent Volume
     */
    Case createCase(NodeRef volumeRef);

    /**
     * close given case
     * @param currentCase
     */
    void closeCase(Case currentCase);

    /**
     * Close all clases that this volume has
     * @param volumeRef
     */
    void closeAllCasesByVolume(NodeRef volumeRef);

    boolean isClosed(Node node);

}
