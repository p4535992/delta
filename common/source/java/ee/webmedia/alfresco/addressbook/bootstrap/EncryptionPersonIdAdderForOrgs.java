<<<<<<< HEAD
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class EncryptionPersonIdAdderForOrgs extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.joinQueryPartsOr(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGANIZATION))));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.ENCRYPTION_PERSON_ID)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.ENCRYPTION_PERSON_ID, null);
            return new String[] { "encryptionPersonIdAspectAdded" };
        }
        return new String[] { "hadAspect" }; // impossible
    }

}
=======
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

public class EncryptionPersonIdAdderForOrgs extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.joinQueryPartsOr(SearchUtil.generateTypeQuery(AddressbookModel.Types.ORGANIZATION))));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.hasAspect(nodeRef, AddressbookModel.Aspects.ENCRYPTION_PERSON_ID)) {
            nodeService.addAspect(nodeRef, AddressbookModel.Aspects.ENCRYPTION_PERSON_ID, null);
            return new String[] { "encryptionPersonIdAspectAdded" };
        }
        return new String[] { "hadAspect" }; // impossible
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
