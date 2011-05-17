package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Finds all compoundWorkflows that have been started and sets SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS to true on the document of the workflow
 * 
 * @author Ats Uiboupin
 */
public class SearchableHasStartedCompoundWorkflowsUpdater extends AbstractNodeUpdater {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SearchableHasStartedCompoundWorkflowsUpdater.class);

    private final List<NodeRef> docsWithStartedCompoundWorkflows = new ArrayList<NodeRef>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                 SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW)
                 , SearchUtil.generatePropertyNotNullQuery(WorkflowCommonModel.Props.STARTED_DATE_TIME)
                ));
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own ContentModel PROP_MODIFIER and PROP_MODIFIED values
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "compoundWorkflowRef", "documentRef", "action performed" };
    }

    @Override
    protected String[] updateNode(NodeRef cWorkflowRef) throws Exception {
        ChildAssociationRef primaryParent = nodeService.getPrimaryParent(cWorkflowRef);
        NodeRef docRef = primaryParent.getParentRef();
        if (docsWithStartedCompoundWorkflows.contains(docRef)) {
            LOG.debug("found document by another compound workflow that is started. Document=" + docRef + "; compoundWorkflow=" + cWorkflowRef);
            return new String[] { docRef.toString(), "Property already set" }; // already set SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS to true based on another
                                                                               // workflow
        }
        Map<QName, Serializable> origDocProps = nodeService.getProperties(docRef);
        Map<QName, Serializable> docProps = new HashMap<QName, Serializable>();
        docProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);
        docProps.put(ContentModel.PROP_MODIFIER, origDocProps.get(ContentModel.PROP_MODIFIER));
        docProps.put(ContentModel.PROP_MODIFIED, origDocProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(docRef, docProps);
        docsWithStartedCompoundWorkflows.add(docRef);
        return new String[] { docRef.toString(), "Property updated" };
    }

}
