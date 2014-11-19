<<<<<<< HEAD
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class TaskCapablePropertyUpdaterV2 extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskCapablePropertyUpdaterV2.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateAspectQuery(AddressbookModel.Aspects.EVERYONE));
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.CONTACT_GROUP));
        String query = SearchUtil.joinQueryPartsOr(queryParts);
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
        List<String> actions = new ArrayList<String>();

        Map<QName, Serializable> taskCapableProp = new HashMap<QName, Serializable>();
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE, null);
            actions.add("taskCapableAspectAdded");
        }
        taskCapableProp.put(AddressbookModel.Props.TASK_CAPABLE, Boolean.FALSE);
        nodeService.addProperties(nodeRef, taskCapableProp);
        actions.add("taskCapablePropertySetToFalse");

        return new String[] { StringUtils.join(actions, ',') };
    }

}
=======
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class TaskCapablePropertyUpdaterV2 extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskCapablePropertyUpdaterV2.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateAspectQuery(AddressbookModel.Aspects.EVERYONE));
        queryParts.add(SearchUtil.generateTypeQuery(AddressbookModel.Types.CONTACT_GROUP));
        String query = SearchUtil.joinQueryPartsOr(queryParts);
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
        List<String> actions = new ArrayList<String>();

        Map<QName, Serializable> taskCapableProp = new HashMap<QName, Serializable>();
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.TASK_CAPABLE, null);
            actions.add("taskCapableAspectAdded");
        }
        taskCapableProp.put(AddressbookModel.Props.TASK_CAPABLE, Boolean.FALSE);
        nodeService.addProperties(nodeRef, taskCapableProp);
        actions.add("taskCapablePropertySetToFalse");

        return new String[] { StringUtils.join(actions, ',') };
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
