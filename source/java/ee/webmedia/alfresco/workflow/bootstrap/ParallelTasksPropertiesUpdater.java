<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Updates parallelTasks property if workflow's paralleTask == null
 * (the situation is actually not allowed in current model, but it somehow appears in live environment)
 * and also rewrites all properties for all tasks in such workflow
 * to force lucene index update (CL task 155454)
 * 
 * @author Riina Tens
 */

public class ParallelTasksPropertiesUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParallelTasksPropertiesUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.WORKFLOW));
        queryParts.add(SearchUtil.generatePropertyNullQuery(WorkflowCommonModel.Props.PARALLEL_TASKS));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef workflowRef) throws Exception {
        Boolean origParallel = (Boolean) nodeService.getProperty(workflowRef, WorkflowCommonModel.Props.PARALLEL_TASKS);
        if (origParallel != null) {
            return new String[] { origParallel.toString() };
        }

        boolean parallel = true;
        QName workflowType = nodeService.getType(workflowRef);
        if (workflowType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
            parallel = false;
        }
        nodeService.setProperty(workflowRef, WorkflowCommonModel.Props.PARALLEL_TASKS, parallel);

        List<String> updatedRefs = new ArrayList<String>();

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(workflowRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef taskRef = childAssoc.getChildRef();
            updatedRefs.add(taskRef.toString());
            nodeService.setProperties(taskRef, nodeService.getProperties(taskRef));
        }

        String[] info = new String[] { "null", Boolean.toString(parallel) };
        return (String[]) ArrayUtils.addAll(info, updatedRefs.toArray(new String[updatedRefs.size()]));
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Updates parallelTasks property if workflow's paralleTask == null
 * (the situation is actually not allowed in current model, but it somehow appears in live environment)
 * and also rewrites all properties for all tasks in such workflow
 * to force lucene index update (CL task 155454)
 */

public class ParallelTasksPropertiesUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParallelTasksPropertiesUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.WORKFLOW));
        queryParts.add(SearchUtil.generatePropertyNullQuery(WorkflowCommonModel.Props.PARALLEL_TASKS));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef workflowRef) throws Exception {
        Boolean origParallel = (Boolean) nodeService.getProperty(workflowRef, WorkflowCommonModel.Props.PARALLEL_TASKS);
        if (origParallel != null) {
            return new String[] { origParallel.toString() };
        }

        boolean parallel = true;
        QName workflowType = nodeService.getType(workflowRef);
        if (workflowType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
            parallel = false;
        }
        nodeService.setProperty(workflowRef, WorkflowCommonModel.Props.PARALLEL_TASKS, parallel);

        List<String> updatedRefs = new ArrayList<String>();

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(workflowRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef taskRef = childAssoc.getChildRef();
            updatedRefs.add(taskRef.toString());
            nodeService.setProperties(taskRef, nodeService.getProperties(taskRef));
        }

        String[] info = new String[] { "null", Boolean.toString(parallel) };
        return (String[]) ArrayUtils.addAll(info, updatedRefs.toArray(new String[updatedRefs.size()]));
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
