package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNotEmptyQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Populates shortRegNr field for registered documents. Updates only SpacesStore.
 * 
 * @author Kaarel Jõgeva
 */
public class ShortRegNumberUpdater extends AbstractNodeUpdater {

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    protected GeneralService generalService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_NUMBER),
                generatePropertyNullQuery(DocumentCommonModel.Props.SHORT_REG_NUMBER)
                ));

        Set<StoreRef> stores = getStores();
        List<ResultSet> result = new ArrayList<ResultSet>(stores.size());
        for (StoreRef storeRef : stores) {
        	result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
		}
        return result;
    }

    protected Set<StoreRef> getStores() {
    	return Collections.singleton(generalService.getStore());
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        final String regNr = (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER);
        final String shortRegNr = StringUtils.substringAfter(regNr, DocumentService.VOLUME_MARK_SEPARATOR);

        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);
        newProps.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, shortRegNr);
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);

        return new String[] { regNr, shortRegNr };
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
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
