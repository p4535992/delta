package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes mimeType on DigiDoc files (CL task 122959)
 * 
 * @author Alar Kvell
 */
public class DdocMimeTypeUpdater extends AbstractNodeUpdater {

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private NodeService nodeService;
    private GeneralService generalService;
    private MimetypeService mimetypeService;

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // TYPE:"cm:content" AND @cm\:name:"*.ddoc"
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(SearchUtil.generateTypeQuery(ContentModel.TYPE_CONTENT));
        queryParts.add(SearchUtil.generatePropertyWildcardQuery(ContentModel.PROP_NAME, ".ddoc", true, true, false));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.exists(nodeRef)) {
            return null;
        }
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        if (name == null || !name.toLowerCase().endsWith(".ddoc")) {
            return null;
        }
        String correctMimeType = mimetypeService.guessMimetype(name);

        ContentData oldContent = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (oldContent == null || correctMimeType.equals(oldContent.getMimetype())) {
            return null;
        }
        ContentData newContent = ContentData.setMimetype(oldContent, correctMimeType);

        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, newContent);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        return new String[] { nodeRef.toString(), name, oldContent.getMimetype(), newContent.getMimetype(), newContent.getEncoding(),
                Long.toString(newContent.getSize()), newContent.getContentUrl() };
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

}
