package ee.webmedia.alfresco.template.bootstrap;

import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getDynamicDocTypes;
import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getStaticToDynamicTypeMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Update template docType property to dynamic type. Should run only when migrating from 2.5 to 3.13.
 */
public class Template25To313DynamicDocTypeUpdater extends AbstractNodeUpdater {

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
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(SearchUtil.generateAspectQuery(DocumentTemplateModel.Aspects.TEMPLATE),
                SearchUtil.generateNotTypeQuery(DocumentTemplateModel.Types.TEMPLATES_ROOT)), false);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String docType = (String) nodeService.getProperty(nodeRef, DocumentTemplateModel.Prop.DOCTYPE_ID);
        Pair<List<String>, List<String>> dynamicDocTypes = getDynamicDocTypes(Collections.singletonList(docType), staticToDynamicDocTypeIds);
        List<String> dynDocTypes = dynamicDocTypes.getFirst();
        nodeService.setProperty(nodeRef, DocumentTemplateModel.Prop.DOCTYPE_ID, (dynDocTypes != null && dynDocTypes.size() > 0) ? dynDocTypes.get(0) : null);
        return dynamicDocTypes.getSecond() != null ? new String[] { TextUtil.joinNonBlankStringsWithComma(dynamicDocTypes.getSecond()) } : null;
    }

}
