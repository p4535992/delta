package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Used to update contract parties. V1 uses fields for two parties, V2 uses child nodes and supports unlimited parties.
 * 
 * @author Kaarel Jõgeva
 */
public class ContractPartyUpdater extends AbstractNodeUpdater {

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private GeneralService generalService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = Arrays.asList(
                SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.CONTRACT_DETAILS),
                SearchUtil.generateTypeQuery(DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE) // Also matches the subtype docspec:contractPartyType
                );
        String query = SearchUtil.joinQueryPartsOr(queryParts);
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
            nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.CONTRACT_PARTY, null);
            actions.add("contractPartyAspectAdded");
            modified = true;
        } else if (aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_PARTY)) {
            actions.add("contractPartyAspectExists");
        }

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

        if (modified) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { StringUtils.join(actions, ",") };
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
