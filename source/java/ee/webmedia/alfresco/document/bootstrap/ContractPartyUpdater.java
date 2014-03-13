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
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Used to update contract parties. V1 uses fields for two parties, V2 uses child nodes and supports unlimited parties.
 */
public class ContractPartyUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE); // Also matches the subtype docspec:contractPartyType
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
        QName type = nodeService.getType(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        List<String> actions = new ArrayList<String>();
        boolean modified = false;

        // contractParty aspect
        if (DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE.equals(type) || DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE.equals(type)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_PARTY)) {
                actions.add("contractPartyAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.CONTRACT_PARTY, null);
                actions.add("contractPartyAspectAdded");
                modified = true;
            }
        }

        if (modified) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { StringUtils.join(actions, ",") };
    }

}
