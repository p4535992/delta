package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Adds docspec:reportDueDate to docspec:errandOrderAbroad and docspec:eventName to docspec:errandApplicationDomestic
 */
public class ErrandAspectUpdater extends AbstractNodeUpdater {

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
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);

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

        if (aspects.contains(DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.REPORT_DUE_DATE)) {
                actions.add("reportDueDateAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.REPORT_DUE_DATE, null);
                actions.add("reportDueDateAspectAdded");
                modified = true;
            }
        }

        if (aspects.contains(DocumentSpecificModel.Aspects.ERRAND_APPLICATION_DOMESTIC)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.EVENT_NAME)) {
                actions.add("eventNameAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.EVENT_NAME, null);
                actions.add("eventNameAspectAdded");
                modified = true;
            }
        }

        return new Pair<Boolean, String>(modified, StringUtils.join(actions, ','));
    }

}
