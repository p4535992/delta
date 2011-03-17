package ee.webmedia.alfresco.addressbook.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.SearchUtil;

public class TaskCapablePropertyUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskCapablePropertyUpdater.class);

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private GeneralService generalService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAspectQuery(AddressbookModel.Aspects.EVERYONE);
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {

        Map<QName, Serializable> taskCapableProp = new HashMap<QName, Serializable>();
        taskCapableProp.put(AddressbookModel.Props.TASK_CAPABLE, Boolean.FALSE);
        nodeService.addProperties(nodeRef, taskCapableProp);

        return null;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
