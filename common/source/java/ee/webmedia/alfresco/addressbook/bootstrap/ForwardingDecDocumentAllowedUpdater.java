package ee.webmedia.alfresco.addressbook.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class ForwardingDecDocumentAllowedUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGANIZATION);
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {

        Map<QName, Serializable> taskCapableProp = new HashMap<QName, Serializable>();
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.FORWARDING_DEC_DOCUMENT_ALLOWED)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.FORWARDING_DEC_DOCUMENT_ALLOWED, null);
        }
        taskCapableProp.put(AddressbookModel.Props.FORWARDING_DEC_DOCUMENT_ALLOWED, Boolean.FALSE);
        nodeService.addProperties(nodeRef, taskCapableProp);

        return null;
    }

}