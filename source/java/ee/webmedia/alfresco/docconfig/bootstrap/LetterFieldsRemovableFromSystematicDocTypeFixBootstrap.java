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
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fix 2 for task 182456 in already existing deployments
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class LetterFieldsRemovableFromSystematicDocTypeFixBootstrap extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                SearchUtil.joinQueryPartsOr(
                        generateStringExactQuery(DocumentCommonModel.Props.KEYWORDS.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID),
                        generateStringExactQuery(DocumentCommonModel.Props.SEND_DESC_VALUE.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                        )));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        Boolean removableFromSystematicDocType = (Boolean) nodeService.getProperty(fieldRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_DOC_TYPE);
        String oldValue = removableFromSystematicDocType == null ? "null" : removableFromSystematicDocType.toString();
        String newValue = "";
        if (!Boolean.TRUE.equals(removableFromSystematicDocType)) {
            removableFromSystematicDocType = Boolean.TRUE;
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_DOC_TYPE, removableFromSystematicDocType);
            newValue = removableFromSystematicDocType.toString();
        }
        return new String[] { oldValue, newValue };
    }

}
