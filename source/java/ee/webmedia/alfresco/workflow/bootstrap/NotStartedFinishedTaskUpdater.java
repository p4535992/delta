<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Used to fix invalid tasks where starDateTime=null and status="lõpetatud" (CL task 158082)
 * 
 * @author Riina tens
 */

public class NotStartedFinishedTaskUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK),
                SearchUtil.generateStringNullQuery(WorkflowCommonModel.Props.STARTED_DATE_TIME),
                SearchUtil.generateStringExactQuery(Status.FINISHED.getName(), WorkflowCommonModel.Props.STATUS)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef taskRef) throws Exception {
        nodeService.setProperty(taskRef, WorkflowCommonModel.Props.STATUS, Status.UNFINISHED.getName());
        return new String[] {};
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Used to fix invalid tasks where starDateTime=null and status="lõpetatud" (CL task 158082)
 */

public class NotStartedFinishedTaskUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK),
                SearchUtil.generateStringNullQuery(WorkflowCommonModel.Props.STARTED_DATE_TIME),
                SearchUtil.generateStringExactQuery(Status.FINISHED.getName(), WorkflowCommonModel.Props.STATUS)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef taskRef) throws Exception {
        nodeService.setProperty(taskRef, WorkflowCommonModel.Props.STATUS, Status.UNFINISHED.getName());
        return new String[] {};
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
