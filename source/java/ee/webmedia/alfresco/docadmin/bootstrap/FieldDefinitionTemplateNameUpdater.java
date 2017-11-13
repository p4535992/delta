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

import ee.webmedia.alfresco.classificator.constant.MappingRestriction;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Changes filedDefinition with ID "case" name from "Asi" to "Teema"
 */
public class FieldDefinitionTemplateNameUpdater extends AbstractNodeUpdater {


    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION),
                generateStringExactQuery("templateName", DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.MAPPING_RESTRICTION, MappingRestriction.MAPPING_FORBIDDEN.name());
        return new String[] { "updateFieldDefinitionTemplateName", MappingRestriction.IDENTICAL_FIELD_MAPPING_ONLY.name() };
    }
}
