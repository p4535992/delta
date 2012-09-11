package ee.webmedia.alfresco.docadmin.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Id of {@link DocumentType} used to be in "docadmin:documentTypeId" property, but now it is in "docadmin:id"
 * 
 * @author Ats Uiboupin
 */
public class DocTypesIdRefactorUpdater extends AbstractNodeUpdater {
    private static final QName OLD_ID = QName.createQName(DocumentAdminModel.URI, "documentTypeId");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.DOCUMENT_TYPE);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef fieldOrFieldDefRef) throws Exception {
        Serializable docTypeId = nodeService.getProperty(fieldOrFieldDefRef, OLD_ID);
        nodeService.setProperty(fieldOrFieldDefRef, DocumentAdminModel.Props.ID, docTypeId);
        nodeService.removeProperty(fieldOrFieldDefRef, OLD_ID);
        return new String[] {};
    }

}
