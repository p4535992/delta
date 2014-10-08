<<<<<<< HEAD
package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Checks for files that have .pdf extension but wrong mimetype
 * 
 * @author Kaarel Jõgeva
 */
public class FileMimetypeUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FileMimetypeUpdater.class);

    private MimetypeService mimetypeService;
    private boolean guessingCorrectly = false;

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ContentModel.TYPE_CONTENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected void executeUpdater() throws Exception {
        String guessMimetype = mimetypeService.guessMimetype("test.pdf");
        guessingCorrectly = MimetypeMap.MIMETYPE_PDF.equals(guessMimetype);
        LOG.info("test.pdf identified as '" + guessMimetype + "', guessingCorrectly=" + guessingCorrectly);
        super.executeUpdater();
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        String displayName = (String) origProps.get(FileModel.Props.DISPLAY_NAME);
        ContentData content = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (content == null) {
            return new String[] { "contentMissing", name, displayName };
        }
        String currentMimetype = content.getMimetype();
        String guessedMimetype = guessingCorrectly ? mimetypeService.guessMimetype(name) : null;
        String newMimetype;

        // Modify if needed
        if (guessingCorrectly && !StringUtils.equalsIgnoreCase(currentMimetype, guessedMimetype)) {
            newMimetype = guessedMimetype;
        } else if (!guessingCorrectly && StringUtils.endsWithIgnoreCase(name, ".pdf") && !StringUtils.equalsIgnoreCase(currentMimetype, MimetypeMap.MIMETYPE_PDF)) {
            newMimetype = MimetypeMap.MIMETYPE_PDF;
        } else {
            return new String[] { "mimetypeNotModified", name, displayName, currentMimetype };
        }

        // Set the mimetype
        content = ContentData.setMimetype(content, newMimetype);

        // Save changes
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, content);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        // Update child association name
        ChildAssociationRef primaryParent = nodeService.getPrimaryParent(nodeRef);
        NodeRef docRef = primaryParent.getParentRef();
        QName oldChildAssocName = primaryParent.getQName();
        QName newChildAssocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
        if (!oldChildAssocName.equals(newChildAssocName)) {
            nodeService.moveNode(nodeRef, docRef, primaryParent.getTypeQName(), newChildAssocName);
        }

        // Update document to reflect changes in ADR
        if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(docRef))) {
            nodeService.setProperty(docRef, ContentModel.PROP_MODIFIED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        }
        return new String[] { "mimetypeModified", name, displayName, currentMimetype, content.getMimetype(),
                oldChildAssocName.toPrefixString(serviceRegistry.getNamespaceService()),
                (newChildAssocName != null ? newChildAssocName.toPrefixString(serviceRegistry.getNamespaceService()) : "") };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

}
=======
package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Checks for files that have .pdf extension but wrong mimetype
 */
public class FileMimetypeUpdater extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FileMimetypeUpdater.class);

    private MimetypeService mimetypeService;
    private boolean guessingCorrectly = false;

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ContentModel.TYPE_CONTENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected void executeUpdater() throws Exception {
        String guessMimetype = mimetypeService.guessMimetype("test.pdf");
        guessingCorrectly = MimetypeMap.MIMETYPE_PDF.equals(guessMimetype);
        LOG.info("test.pdf identified as '" + guessMimetype + "', guessingCorrectly=" + guessingCorrectly);
        super.executeUpdater();
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        String displayName = (String) origProps.get(FileModel.Props.DISPLAY_NAME);
        ContentData content = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (content == null) {
            return new String[] { "contentMissing", name, displayName };
        }
        String currentMimetype = content.getMimetype();
        String guessedMimetype = guessingCorrectly ? mimetypeService.guessMimetype(name) : null;
        String newMimetype;

        // Modify if needed
        if (guessingCorrectly && !StringUtils.equalsIgnoreCase(currentMimetype, guessedMimetype)) {
            newMimetype = guessedMimetype;
        } else if (!guessingCorrectly && StringUtils.endsWithIgnoreCase(name, ".pdf") && !StringUtils.equalsIgnoreCase(currentMimetype, MimetypeMap.MIMETYPE_PDF)) {
            newMimetype = MimetypeMap.MIMETYPE_PDF;
        } else {
            return new String[] { "mimetypeNotModified", name, displayName, currentMimetype };
        }

        // Set the mimetype
        content = ContentData.setMimetype(content, newMimetype);

        // Save changes
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, content);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        // Update child association name
        ChildAssociationRef primaryParent = nodeService.getPrimaryParent(nodeRef);
        NodeRef docRef = primaryParent.getParentRef();
        QName oldChildAssocName = primaryParent.getQName();
        QName newChildAssocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
        if (!oldChildAssocName.equals(newChildAssocName)) {
            nodeService.moveNode(nodeRef, docRef, primaryParent.getTypeQName(), newChildAssocName);
        }

        // Update document to reflect changes in ADR
        if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(docRef))) {
            nodeService.setProperty(docRef, ContentModel.PROP_MODIFIED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        }
        return new String[] { "mimetypeModified", name, displayName, currentMimetype, content.getMimetype(),
                oldChildAssocName.toPrefixString(serviceRegistry.getNamespaceService()),
                (newChildAssocName != null ? newChildAssocName.toPrefixString(serviceRegistry.getNamespaceService()) : "") };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

}
>>>>>>> develop-5.1
