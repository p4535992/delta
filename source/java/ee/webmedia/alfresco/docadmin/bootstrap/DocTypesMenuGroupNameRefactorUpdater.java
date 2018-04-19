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
 * MenuGroupName of {@link DocumentType} used to be in "docadmin:documentTypeGroup" property, but now it is in "docadmin:menuGroupName"
 */
public class DocTypesMenuGroupNameRefactorUpdater extends AbstractNodeUpdater {
    private static final QName OLD_MENU_GROUP_NAME = QName.createQName(DocumentAdminModel.URI, "documentTypeGroup");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.DOCUMENT_TYPE);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef fieldOrFieldDefRef) throws Exception {
        Serializable docTypeId = nodeService.getProperty(fieldOrFieldDefRef, OLD_MENU_GROUP_NAME);
        nodeService.setProperty(fieldOrFieldDefRef, DocumentAdminModel.Props.MENU_GROUP_NAME, docTypeId);
        nodeService.removeProperty(fieldOrFieldDefRef, OLD_MENU_GROUP_NAME);
        return new String[] {};
    }

}
