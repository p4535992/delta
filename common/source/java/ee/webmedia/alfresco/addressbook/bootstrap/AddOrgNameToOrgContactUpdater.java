package ee.webmedia.alfresco.addressbook.bootstrap;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Keit Tehvan
 */
public class AddOrgNameToOrgContactUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON)));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        NodeRef org = nodeService.getPrimaryParent(nodeRef).getParentRef();
        Serializable orgName = nodeService.getProperty(org, AddressbookModel.Props.ORGANIZATION_NAME);
        nodeService.setProperty(nodeRef, AddressbookModel.Props.PRIVATE_PERSON_ORG_NAME, orgName);
        return new String[] { "set to " + orgName };
    }
}
