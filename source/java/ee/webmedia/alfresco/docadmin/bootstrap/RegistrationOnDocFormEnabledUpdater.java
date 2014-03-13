package ee.webmedia.alfresco.docadmin.bootstrap;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Set registrationOnDocFormEnabled property on all document types.
 */
public class RegistrationOnDocFormEnabledUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.DOCUMENT_TYPE);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Boolean registrationEnbled = Boolean.TRUE.equals(nodeService.getProperty(nodeRef, DocumentAdminModel.Props.REGISTRATION_ENABLED));
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.REGISTRATION_ON_DOC_FORM_ENABLED, registrationEnbled);
        return new String[] { registrationEnbled.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "registrationOnDocFormEnabled" };
    }

}
