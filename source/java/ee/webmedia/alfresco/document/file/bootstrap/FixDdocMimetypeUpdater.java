package ee.webmedia.alfresco.document.file.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

public class FixDdocMimetypeUpdater extends AbstractNodeUpdater {

    private MimetypeService mimetypeService;
    private BulkLoadNodeService bulkLoadNodeService;

    private Map<NodeRef, Node> fileNodes;

    @Override
    protected void executeUpdater() throws Exception {
        String guessMimetype = mimetypeService.guessMimetype("file.ddoc");
        boolean guessingCorrectly = StringUtils.equalsIgnoreCase(SignatureService.DIGIDOC_MIMETYPE, guessMimetype);
        if (guessingCorrectly) {
            super.executeUpdater();
        } else {
            log.warn("Not stating updater because .ddoc file was identified as " + guessMimetype);
        }
        fileNodes = null;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(ContentModel.TYPE_CONTENT),
                SearchUtil.generatePropertyWildcardQuery(ContentModel.PROP_NAME, FilenameUtil.DDOC_EXTENSION, true, false));
        List<ResultSet> result = new ArrayList<>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
    }

    @Override
    protected void doBeforeBatchUpdate(List<NodeRef> batchList) {
        Set<QName> propsToLoad = new HashSet<>(Arrays.asList(ContentModel.PROP_CONTENT, ContentModel.PROP_NAME));
        fileNodes = bulkLoadNodeService.loadNodes(batchList, propsToLoad);
    }

    @Override
    protected String[] updateNode(NodeRef fileRef) throws Exception {
        Node fileNode = fileNodes.get(fileRef);
        if (fileNode == null) {
            log.warn("Did not find node: " + fileRef);
            return new String[] { "nodeNotFound" };
        }
        Map<String, Object> props = fileNode.getProperties();
        ContentData content = (ContentData) props.get(ContentModel.PROP_CONTENT);
        String name = (String) props.get(ContentModel.PROP_NAME);
        if (content == null) {
            return new String[] { "contentMissing", name };
        }
        String currentMimetype = content.getMimetype();
        String guessedMimeType = mimetypeService.guessMimetype(name);

        if (!SignatureService.DIGIDOC_MIMETYPE.equalsIgnoreCase(currentMimetype) && SignatureService.DIGIDOC_MIMETYPE.equalsIgnoreCase(guessedMimeType)) {
            content = ContentData.setMimetype(content, SignatureService.DIGIDOC_MIMETYPE);
            nodeService.setProperty(fileRef, ContentModel.PROP_CONTENT, content);
            return new String[] { "mimetypeChanged", name, currentMimetype, guessedMimeType };
        }
        return new String[] { "notModified", name, currentMimetype, guessedMimeType };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
