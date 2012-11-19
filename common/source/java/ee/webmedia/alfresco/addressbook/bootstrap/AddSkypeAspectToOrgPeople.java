package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class AddSkypeAspectToOrgPeople extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGPERSON)));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.SKYPE)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.SKYPE, null);
            return new String[] { "addedSkypeAspect" };
        }
        return new String[] { "hadSkypeAspect" };
    }

}
