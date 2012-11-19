package ee.webmedia.alfresco.docconfig.bootstrap;

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

/**
 * Case is no longer a mandatory field (CL 195692)
 * 
 * @author Kaarel Jõgeva
 */
public class CaseFieldNotMandatoryUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD)
                , generateStringExactQuery(DocumentCommonModel.Props.CASE.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        Boolean mandatory = (Boolean) nodeService.getProperty(fieldRef, DocumentAdminModel.Props.MANDATORY);
        String oldValue = mandatory == null ? "null" : mandatory.toString();
        String newValue = "";
        if (Boolean.TRUE.equals(mandatory)) {
            mandatory = Boolean.FALSE;
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.MANDATORY, mandatory);
            newValue = mandatory.toString();
        }
        return new String[] { oldValue, newValue };
    }

}
