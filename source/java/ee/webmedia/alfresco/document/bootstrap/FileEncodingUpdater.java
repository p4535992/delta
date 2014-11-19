package ee.webmedia.alfresco.document.bootstrap;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes encoding on HTML files (CL task 196507)
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class FileEncodingUpdater extends AbstractNodeUpdater {

    private MimetypeService mimetypeService;

    private final Set<NodeRef> documentsToUpdate = new HashSet<NodeRef>();

    public Set<NodeRef> getDocumentsToUpdate() {
        return documentsToUpdate;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(ContentModel.TYPE_CONTENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
<<<<<<< HEAD
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
=======
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        if (StringUtils.isEmpty(name)) {
            return new String[] { "fileNameIsEmpty" };
        }

        ContentData oldContent = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (oldContent == null) {
            return new String[] { "contentDataIsNull", name };
        }

        String mimetype = oldContent.getMimetype();
        String oldEncoding = oldContent.getEncoding();
        if (!MimetypeMap.MIMETYPE_HTML.equalsIgnoreCase(mimetype)) {
            return new String[] { "mimetypeIsNotHtml", name, mimetype, oldEncoding, oldEncoding,
                    Long.toString(oldContent.getSize()), oldContent.getContentUrl() };
        }

        InputStream is = serviceRegistry.getFileFolderService().getReader(nodeRef).getContentInputStream();
        String newEncoding;
        try {
            newEncoding = mimetypeService.getContentCharsetFinder().getCharset(is, mimetype).name();
        } finally {
            is.close();
        }

        if (newEncoding.equalsIgnoreCase(oldEncoding)) {
            return new String[] { "encodingIsCorrect", name, mimetype, oldEncoding, newEncoding,
                    Long.toString(oldContent.getSize()), oldContent.getContentUrl() };
        }

        ContentData newContent = ContentData.setEncoding(oldContent, newEncoding);

        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, newContent);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        NodeRef documentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();

        QName type = nodeService.getType(documentRef);
        String typeString = type.toPrefixString(serviceRegistry.getNamespaceService());
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "encodingUpdatedAndParentIsNotDocument", name, mimetype, oldEncoding, newEncoding,
                    Long.toString(newContent.getSize()), newContent.getContentUrl(), typeString };
        }
        if (!nodeService.hasAspect(documentRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            return new String[] { "encodingUpdatedAndParentDocumentIsNotSearchable", name, mimetype, oldEncoding, newEncoding,
                    Long.toString(newContent.getSize()), newContent.getContentUrl(), typeString };
        }

        documentsToUpdate.add(documentRef);
        return new String[] { "encodingUpdatedAndParentDocumentUpdateQueued", name, mimetype, oldEncoding, newEncoding,
                Long.toString(newContent.getSize()), newContent.getContentUrl(), typeString, documentRef.toString() };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

}
