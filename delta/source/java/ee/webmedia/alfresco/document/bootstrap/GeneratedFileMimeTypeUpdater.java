package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes mimeType on files (CL task 151423)
 * 
 * @author Alar Kvell
 */
public class GeneratedFileMimeTypeUpdater extends AbstractNodeUpdater {

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
        String query = SearchUtil.generateStringExactQuery("true", File.GENERATED);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!nodeService.exists(nodeRef)) {
            return new String[] { nodeRef.toString(), "nodeDoesNotExist" };
        }
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        if (StringUtils.isEmpty(name)) {
            return new String[] { nodeRef.toString(), "fileNameIsEmpty" };
        }
        String correctMimeType = mimetypeService.guessMimetype(name);

        ContentData oldContent = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (oldContent == null) {
            return new String[] { nodeRef.toString(), "contentDataIsNull", name };
        }
        if (correctMimeType.equals(oldContent.getMimetype())) {
            return new String[] { nodeRef.toString(), "mimetypeIsCorrect", name, oldContent.getMimetype(), oldContent.getMimetype(), oldContent.getEncoding(),
                    Long.toString(oldContent.getSize()), oldContent.getContentUrl() };
        }
        ContentData newContent = ContentData.setMimetype(oldContent, correctMimeType);

        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, newContent);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        // Update modified time on document, so ADR would detect up changes
        NodeRef documentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        origProps = nodeService.getProperties(documentRef);
        setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, new Date());
        nodeService.addProperties(documentRef, setProps);
        
        return new String[] { nodeRef.toString(), "mimetypeUpdated", name, oldContent.getMimetype(), newContent.getMimetype(), newContent.getEncoding(),
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