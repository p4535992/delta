<<<<<<< HEAD
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class TaskCapablePropertyRemoverOnPeople extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON));
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.PRIV_PERSON));
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, SearchUtil.joinQueryPartsOr(queryParts)));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE)) {
            nodeService.removeAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE);
            return new String[] { "taskCapableAspectRemoved" };
        }
        return new String[] { "didNotHaveAspect" };
    }

}
=======
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class TaskCapablePropertyRemoverOnPeople extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON));
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.PRIV_PERSON));
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, SearchUtil.joinQueryPartsOr(queryParts)));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE)) {
            nodeService.removeAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE);
            return new String[] { "taskCapableAspectRemoved" };
        }
        return new String[] { "didNotHaveAspect" };
    }

}
>>>>>>> develop-5.1
