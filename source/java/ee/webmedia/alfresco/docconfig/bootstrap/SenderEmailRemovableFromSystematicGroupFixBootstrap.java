package ee.webmedia.alfresco.docconfig.bootstrap;

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
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fix for task 182461 in already existing deployments
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class SenderEmailRemovableFromSystematicGroupFixBootstrap extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                SearchUtil.generateStringExactQuery(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        Boolean removableFromSystematicGroup = (Boolean) nodeService.getProperty(fieldRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP);
        String oldValue = removableFromSystematicGroup == null ? "null" : removableFromSystematicGroup.toString();
        String newValue = "";
        if (!Boolean.FALSE.equals(removableFromSystematicGroup)) {
            removableFromSystematicGroup = Boolean.FALSE;
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, removableFromSystematicGroup);
            newValue = removableFromSystematicGroup.toString();
        }
        return new String[] { oldValue, newValue };
    }

}
