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

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Removes docspec:template aspect from docsub:vacationOrder documents
 */
public class VacationOrderTemplateAspectUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = Arrays.asList(
                      SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.VACATION_ORDER),
                      SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.TEMPLATE)
                      );
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
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
        QName type = nodeService.getType(nodeRef);

        Pair<Boolean, String> result = updateDocument(nodeRef, type, aspects);

        if (result.getFirst()) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { result.getSecond() };
    }

    public Pair<Boolean, String> updateDocument(NodeRef nodeRef, QName type, Set<QName> aspects) {
        if (!DocumentSubtypeModel.Types.VACATION_ORDER.equals(type)) {
            return new Pair<Boolean, String>(false, "isNotVacationOrderType");
        }
        if (!aspects.contains(DocumentSpecificModel.Aspects.TEMPLATE)) {
            return new Pair<Boolean, String>(false, "doesNotHaveTemplateAspect");
        }
        nodeService.removeAspect(nodeRef, DocumentSpecificModel.Aspects.TEMPLATE);
        return new Pair<Boolean, String>(true, "templateAspectRemoved");
    }

}
