package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Adds docspec:reportDueDate to docspec:errandOrderAbroad and docspec:eventName to docspec:errandApplicationDomestic
 * 
 * @author Kaarel Jõgeva
 */
public class ErrandAspectUpdater extends AbstractNodeUpdater {

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private GeneralService generalService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = Arrays.asList(
                      SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD),
                      SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC)
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
        boolean modified = false;
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        List<String> actions = new ArrayList<String>(2);

        if (nodeService.hasAspect(nodeRef, DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.REPORT_DUE_DATE, null);
            actions.add("reportDueDateAspectAdded");
            modified = true;
        }

        if (nodeService.hasAspect(nodeRef, DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.EVENT_NAME, null);
            actions.add("eventNameAspectAdded");
            modified = true;
        }

        if (modified) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { StringUtils.join(actions, ',') };
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
