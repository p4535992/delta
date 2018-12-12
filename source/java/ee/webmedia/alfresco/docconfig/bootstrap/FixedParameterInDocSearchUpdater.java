package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class FixedParameterInDocSearchUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION, DocumentAdminModel.Types.FIELD),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.IS_FIXED_PARAMETER_IN_DOC_SEARCH, true)
        );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.IS_FIXED_PARAMETER_IN_DOC_SEARCH, false);
        return null;
    }
}
