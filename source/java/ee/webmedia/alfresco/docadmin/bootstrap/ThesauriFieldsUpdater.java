package ee.webmedia.alfresco.docadmin.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * There were {@link FieldType}s {@link FieldType#HIERARCHICAL_KEYWORD_LEVEL1} and {@link FieldType#HIERARCHICAL_KEYWORD_LEVEL2} that were deleted.
 * This updater updates fieldTypes of {@link Field} and {@link FieldDefinition} objects
 * 
 * @author Ats Uiboupin
 */

public class ThesauriFieldsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                joinQueryPartsOr(
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD),
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION)
                )
                , joinQueryPartsOr(
                        generatePropertyExactQuery(DocumentAdminModel.Props.FIELD_TYPE, "HIERARCHICAL_KEYWORD_LEVEL1", false),
                        generatePropertyExactQuery(DocumentAdminModel.Props.FIELD_TYPE, "HIERARCHICAL_KEYWORD_LEVEL2", false)
                        )
                ));
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef fieldOrFieldDefRef) throws Exception {
        nodeService.setProperty(fieldOrFieldDefRef, DocumentAdminModel.Props.FIELD_TYPE, FieldType.TEXT_FIELD.name());
        return new String[] {};
    }

}
