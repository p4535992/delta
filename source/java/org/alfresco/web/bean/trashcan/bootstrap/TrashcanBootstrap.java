<<<<<<< HEAD
package org.alfresco.web.bean.trashcan.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Priit Pikk
 */
public class TrashcanBootstrap extends AbstractNodeUpdater {

    private DocumentService documentService;
    private NodeService nodeService;
    private DocumentListService documentListService;
    private UserService userService;
    private TransactionService transactionService;

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Arrays.asList(searchService.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, "ASPECT:\"" + ContentModel.ASPECT_ARCHIVED + "\""));
    }

    @Override
    protected String[] updateNode(final NodeRef nodeRef) throws Exception {
        boolean delete = true;
        ChildAssociationRef origChildRef = (ChildAssociationRef) nodeService.getProperties(nodeRef).get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
        QName type = nodeService.getType(nodeRef);
        boolean nodeExists = nodeService.exists(origChildRef.getParentRef());
        if (nodeExists && DocumentCommonModel.Types.DOCUMENT.equals(type)
                && !DocumentCommonModel.Types.DRAFTS.equals(nodeService.getType(origChildRef.getParentRef()))) {
            delete = false;
        }
        if (ContentModel.TYPE_CONTENT.equals(type)) {
            delete = false;
        }
        String log[];
        if (delete) {
            nodeService.deleteNode(nodeRef);
            log = new String[] { "Deleted" };
        } else {
            Map<QName, Serializable> oldProperties = nodeService.getProperties(nodeRef);
            Map<QName, Serializable> newProperties = new HashMap<QName, Serializable>();
            newProperties.put(ContentModel.PROP_ARCHIVED_BY_NAME, userService.getUserFullName((String) oldProperties.get(ContentModel.PROP_ARCHIVED_BY)));
            if (nodeExists) {
                newProperties.put(ContentModel.PROP_ARCHIVED_ORIGINAL_LOCATION_STRING, documentListService.getDisplayPath(origChildRef.getParentRef(), true));
            }
            if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(DocumentCommonModel.Props.DOC_NAME));
                newProperties
                        .put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, documentService.getDocumentByNodeRef(nodeRef).getDocumentTypeName());
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "document");
            } else if (ContentModel.TYPE_CONTENT.equals(type) && oldProperties.containsKey(DocumentTemplateModel.Prop.TEMPLATE_TYPE)) {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(ContentModel.PROP_NAME));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, oldProperties.get(DocumentTemplateModel.Prop.COMMENT));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "content");
            } else {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(ContentModel.PROP_NAME));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, MessageUtil.getMessage("trashcan_file_type"));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "file");
            }
            nodeService.addProperties(nodeRef, newProperties);
            log = new String[] { "DeletedNodeChanged", newProperties.toString() };
        }
        return log;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}
=======
package org.alfresco.web.bean.trashcan.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class TrashcanBootstrap extends AbstractNodeUpdater {

    private DocumentService documentService;
    private NodeService nodeService;
    private DocumentListService documentListService;
    private UserService userService;
    private TransactionService transactionService;

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return Arrays.asList(searchService.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, "ASPECT:\"" + ContentModel.ASPECT_ARCHIVED + "\""));
    }

    @Override
    protected String[] updateNode(final NodeRef nodeRef) throws Exception {
        boolean delete = true;
        ChildAssociationRef origChildRef = (ChildAssociationRef) nodeService.getProperties(nodeRef).get(ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
        QName type = nodeService.getType(nodeRef);
        boolean nodeExists = nodeService.exists(origChildRef.getParentRef());
        if (nodeExists && DocumentCommonModel.Types.DOCUMENT.equals(type)
                && !DocumentCommonModel.Types.DRAFTS.equals(nodeService.getType(origChildRef.getParentRef()))) {
            delete = false;
        }
        if (ContentModel.TYPE_CONTENT.equals(type)) {
            delete = false;
        }
        String log[];
        if (delete) {
            nodeService.deleteNode(nodeRef);
            log = new String[] { "Deleted" };
        } else {
            Map<QName, Serializable> oldProperties = nodeService.getProperties(nodeRef);
            Map<QName, Serializable> newProperties = new HashMap<QName, Serializable>();
            newProperties.put(ContentModel.PROP_ARCHIVED_BY_NAME, userService.getUserFullName((String) oldProperties.get(ContentModel.PROP_ARCHIVED_BY)));
            if (nodeExists) {
                newProperties.put(ContentModel.PROP_ARCHIVED_ORIGINAL_LOCATION_STRING, documentListService.getDisplayPath(origChildRef.getParentRef(), true));
            }
            if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(DocumentCommonModel.Props.DOC_NAME));
                newProperties
                        .put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, documentService.getDocumentByNodeRef(nodeRef).getDocumentTypeName());
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "document");
            } else if (ContentModel.TYPE_CONTENT.equals(type) && oldProperties.containsKey(DocumentTemplateModel.Prop.TEMPLATE_TYPE)) {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(ContentModel.PROP_NAME));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, oldProperties.get(DocumentTemplateModel.Prop.COMMENT));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "content");
            } else {
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_NAME, oldProperties.get(ContentModel.PROP_NAME));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE_STRING, MessageUtil.getMessage("trashcan_file_type"));
                newProperties.put(ContentModel.PROP_ARCHIVED_OBJECT_TYPE, "file");
            }
            nodeService.addProperties(nodeRef, newProperties);
            log = new String[] { "DeletedNodeChanged", newProperties.toString() };
        }
        return log;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
