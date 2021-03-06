package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class SubstituteFieldDefinitionsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION)
                , joinQueryPartsOr(generateStringExactQuery("substituteName", DocumentAdminModel.Props.FIELD_ID),
                        generateStringExactQuery("substitutionBeginDate", DocumentAdminModel.Props.FIELD_ID),
                        generateStringExactQuery("substitutionEndDate", DocumentAdminModel.Props.FIELD_ID))
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, Boolean.TRUE);
        return new String[] { "updated" };
    }

}
