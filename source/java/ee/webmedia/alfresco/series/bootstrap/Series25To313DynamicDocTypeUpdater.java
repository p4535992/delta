package ee.webmedia.alfresco.series.bootstrap;

import static ee.webmedia.alfresco.document.bootstrap.ConvertToDynamicDocumentsUpdater.STATIC_TO_DYNAMIC_DOC_TYPE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Update series docType property to dynamic type. Should run only when migrating from 2.5 to 3.13.
 * 
 * @author Riina Tens
 */
public class Series25To313DynamicDocTypeUpdater extends AbstractNodeUpdater {

    private Map<String, String> staticToDynamicDocTypeIds;

    @Override
    protected void executeInternal() throws Throwable {
        if (isEnabled()) {
            Set<String> dynamicDocTypes = BeanHelper.getDocumentAdminService().getDocumentTypeNames(null).keySet();
            staticToDynamicDocTypeIds = getStaticToDynamicTypeMapping(dynamicDocTypes, BeanHelper.getDictionaryService().getTypes(DocumentSubtypeModel.MODEL_NAME));
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    public static Map<String, String> getStaticToDynamicTypeMapping(Set<String> dynamicDocTypes, Collection<QName> docSubTypes) {
        Map<String, String> staticToDynamicDocTypeIds2 = new HashMap<String, String>();
        for (QName staticDocType : docSubTypes) {
            String dynamicDocTypeId = staticDocType.getLocalName();
            if (STATIC_TO_DYNAMIC_DOC_TYPE.containsKey(staticDocType)) {
                staticToDynamicDocTypeIds2.put(staticDocType.toString(), STATIC_TO_DYNAMIC_DOC_TYPE.get(staticDocType));
                continue;
            }
            if (dynamicDocTypes.contains(dynamicDocTypeId)) {
                staticToDynamicDocTypeIds2.put(staticDocType.toString(), dynamicDocTypeId);
            }
        }
        return staticToDynamicDocTypeIds2;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        @SuppressWarnings("unchecked")
        List<String> docTypes = (List<String>) nodeService.getProperty(nodeRef, SeriesModel.Props.DOC_TYPE);
        Pair<List<String>, List<String>> dynamicDocTypes = getDynamicDocTypes(docTypes, staticToDynamicDocTypeIds);
        nodeService.setProperty(nodeRef, SeriesModel.Props.DOC_TYPE, (Serializable) dynamicDocTypes.getFirst());
        return dynamicDocTypes.getSecond() != null ? new String[] { TextUtil.joinNonBlankStringsWithComma(dynamicDocTypes.getSecond()) } : null;
    }

    public static Pair<List<String>, List<String>> getDynamicDocTypes(List<String> docTypes, Map<String, String> staticToDynamicDocTypeIds) {
        List<String> dynamicDocTypes = null;
        List<String> omittedDocTypes = null;
        if (docTypes != null) {
            dynamicDocTypes = new ArrayList<String>();
            omittedDocTypes = new ArrayList<String>();
            for (String staticDocType : docTypes) {
                if (staticToDynamicDocTypeIds.containsKey(staticDocType)) {
                    dynamicDocTypes.add(staticToDynamicDocTypeIds.get(staticDocType));
                } else {
                    omittedDocTypes.add(staticDocType);
                }
            }
        }
        return new Pair<List<String>, List<String>>(dynamicDocTypes, omittedDocTypes);
    }
}
