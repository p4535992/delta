package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Set access restriction reason, restriction begin and end date and ending description to null
 * for documents where access restriction="Avalik" or "Majasisene"
 * 
 * @author Riina Tens
 */
public class DocumentAccessRestrictionUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generatePropertyExactQuery(DocumentCommonModel.Props.ACCESS_RESTRICTION,
                        Arrays.asList(AccessRestriction.OPEN.getValueName(), AccessRestriction.INTERNAL.getValueName())));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String accessRestriction = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        return updateNode(nodeRef, accessRestriction);
    }

    protected String[] updateNode(NodeRef nodeRef, String accessRestriction) {
        if (AccessRestriction.OPEN.equals(accessRestriction) || AccessRestriction.INTERNAL.equals(accessRestriction)) {
            Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
            propsToAdd.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, null);
            propsToAdd.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, null);
            propsToAdd.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, null);
            propsToAdd.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, null);
            nodeService.addProperties(nodeRef, propsToAdd);
            return new String[] { "Set null" };
        }
        return new String[] { "Not modified, accessRestriction value: " + accessRestriction };
    }
}
