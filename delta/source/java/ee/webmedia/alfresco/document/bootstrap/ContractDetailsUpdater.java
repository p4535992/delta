package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Used to update contract details.
 * 
 * @author Kaarel Jõgeva
 */
public class ContractDetailsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.CONTRACT_DETAILS);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);

        Pair<Boolean, String> result = updateDocument(nodeRef, aspects);

        if (result.getFirst()) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { result.getSecond() };
    }

    public Pair<Boolean, String> updateDocument(NodeRef nodeRef, Set<QName> aspects) {
        boolean modified = false;
        List<String> actions = new ArrayList<String>();

        // Contract property versioning
        if (aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_DETAILS)) {
            if (!aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1) && !aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V2)) {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1, null);
                actions.add("contractDetailsV1AspectAdded");
                modified = true;
            } else {
                actions.add("contractDetailsV1OrV2AspectPresent");
            }
        }

        return new Pair<Boolean, String>(modified, StringUtils.join(actions, ','));
    }

}
