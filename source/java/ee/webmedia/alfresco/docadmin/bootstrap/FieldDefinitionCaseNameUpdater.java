package ee.webmedia.alfresco.docadmin.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Changes filedDefinition with ID "case" name from "Asi" to "Teema"
 * 
 * @author Vladimir Drozdik
 */
public class FieldDefinitionCaseNameUpdater extends AbstractNodeUpdater {

    private final String fieldDefinitionName = MessageUtil.getMessage("case");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                generateStringExactQuery(DocumentCommonModel.Props.CASE.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID),
                generateStringExactQuery("Asi", DocumentAdminModel.Props.NAME)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        String oldValue = (String) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.NAME);
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.NAME, fieldDefinitionName);
        return new String[] { "updateFieldDefinitionCaseName", oldValue };
    }
}
