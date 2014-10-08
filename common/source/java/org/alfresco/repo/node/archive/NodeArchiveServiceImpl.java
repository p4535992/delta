/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.node.archive;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.archive.RestoreNodeReport.RestoreStatus;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Implementation of the node archive abstraction.
 * 
 * @author Derek Hulley
 */
public class NodeArchiveServiceImpl implements NodeArchiveService
{
    private static Log logger = LogFactory.getLog(NodeArchiveServiceImpl.class);
    
    private NodeService nodeService;
    private SearchService searchService;
    private TransactionService transactionService;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public NodeRef getStoreArchiveNode(StoreRef originalStoreRef)
    {
        return nodeService.getStoreArchiveNode(originalStoreRef);
    }

    public NodeRef getArchivedNode(NodeRef originalNodeRef)
    {
        StoreRef orginalStoreRef = originalNodeRef.getStoreRef();
        NodeRef archiveRootNodeRef = nodeService.getStoreArchiveNode(orginalStoreRef);
        // create the likely location of the archived node
        NodeRef archivedNodeRef = new NodeRef(
                archiveRootNodeRef.getStoreRef(),
                originalNodeRef.getId());
        return archivedNodeRef;
    }

    /**
     * Get all the nodes that were archived <b>from</b> the given store.
     */
    private ResultSet getArchivedNodes(StoreRef originalStoreRef)
    {
        // Get the archive location
        NodeRef archiveParentNodeRef = nodeService.getStoreArchiveNode(originalStoreRef);
        StoreRef archiveStoreRef = archiveParentNodeRef.getStoreRef();
        // build the query
        String query = String.format("PARENT:\"%s\" AND ASPECT:\"%s\"", archiveParentNodeRef, ContentModel.ASPECT_ARCHIVED);
        // search parameters
        SearchParameters params = new SearchParameters();
        params.addStore(archiveStoreRef);
        params.setLanguage(SearchService.LANGUAGE_LUCENE);
        params.setQuery(query);
//        params.addSort(ContentModel.PROP_ARCHIVED_DATE.toString(), false);
        // get all archived children using a search
        ResultSet rs = searchService.query(params);
        // done
        return rs;
    }

    /**
     * This is the primary restore method that all <code>restore</code> methods fall back on.
     * It executes the restore for the node in a separate transaction and attempts to catch
     * the known conditions that can be reported back to the client.
     */
    public RestoreNodeReport restoreArchivedNode(
            final NodeRef archivedNodeRef,
            final NodeRef destinationNodeRef,
            final QName assocTypeQName,
            final QName assocQName)
    {
        RestoreNodeReport report = new RestoreNodeReport(archivedNodeRef);
        report.setTargetParentNodeRef(destinationNodeRef);
        try
        {
            // Transactional wrapper to attempt the restore
            RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
            RetryingTransactionCallback<NodeRef> restoreCallback = new RetryingTransactionCallback<NodeRef>()
            {
                public NodeRef execute() throws Exception
                {
                    NodeRef restoredNodeRef = nodeService.restoreNode(archivedNodeRef, destinationNodeRef, assocTypeQName, assocQName);
                    BeanHelper.getDocumentLogService().addDeletedObjectLog(restoredNodeRef, "applog_delete_restore");
                    if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(restoredNodeRef))) {
                        DocumentService documentService = BeanHelper.getDocumentService();
                        documentService.updateParentNodesContainingDocsCount(restoredNodeRef, true);
                        String regNumber = (String) nodeService.getProperty(restoredNodeRef, REG_NUMBER);
                        documentService.updateParentDocumentRegNumbers(restoredNodeRef, null, regNumber);
                    }                    
                    return restoredNodeRef;
                }
            };
            NodeRef newNodeRef = txnHelper.doInTransaction(restoreCallback, false, true);
            // success
            report.setRestoredNodeRef(newNodeRef);
            report.setStatus(RestoreStatus.SUCCESS);

            // Remove deleted document entry for ADR sync
            if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(newNodeRef))) { 
                List<NodeRef> searchAdrDeletedDocument = BeanHelper.getDocumentSearchService().searchAdrDeletedDocument(newNodeRef);
                if (!searchAdrDeletedDocument.isEmpty()) {
                    for (NodeRef deletedAdrEntry : searchAdrDeletedDocument) {
                        nodeService.deleteNode(deletedAdrEntry);
                    }
                }
            }
        }
        catch (InvalidNodeRefException e)
        {
            report.setCause(e);
            NodeRef invalidNodeRef = e.getNodeRef();
            if (archivedNodeRef.equals(invalidNodeRef))
            {
                // not too serious, but the node to archive is missing
                report.setStatus(RestoreStatus.FAILURE_INVALID_ARCHIVE_NODE);
            }
            else if (EqualsHelper.nullSafeEquals(destinationNodeRef, invalidNodeRef))
            {
                report.setStatus(RestoreStatus.FAILURE_INVALID_PARENT);
            }
            else if (destinationNodeRef == null)
            {
                // get the original parent of the archived node
                ChildAssociationRef originalParentAssocRef = (ChildAssociationRef) nodeService.getProperty(
                        archivedNodeRef,
                        ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
                NodeRef originalParentNodeRef = originalParentAssocRef.getParentRef();
                if (EqualsHelper.nullSafeEquals(originalParentNodeRef, invalidNodeRef))
                {
                    report.setStatus(RestoreStatus.FAILURE_INVALID_PARENT);
                }
                else
                {
                    // some other invalid node was detected
                    report.setStatus(RestoreStatus.FAILURE_OTHER);
                }
            }
            else
            {
                // some other invalid node was detected
                report.setStatus(RestoreStatus.FAILURE_OTHER);
            }
        }
        catch (AccessDeniedException e)
        {
            report.setCause(e);
            report.setStatus(RestoreStatus.FAILURE_PERMISSION);
        }
        catch (Throwable e)
        {
            report.setCause(e);
            report.setStatus(RestoreStatus.FAILURE_OTHER);
            logger.error("An unhandled exception stopped the restore", e);
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Attempted node restore: "+ report);
        }
        return report;
    }

    /**
     * @see #restoreArchivedNode(NodeRef, NodeRef, QName, QName)
     */
    public RestoreNodeReport restoreArchivedNode(NodeRef archivedNodeRef)
    {
        return restoreArchivedNode(archivedNodeRef, null, null, null);
    }

    /**
     * @see #restoreArchivedNodes(List, NodeRef, QName, QName)
     */
    public List<RestoreNodeReport> restoreArchivedNodes(List<NodeRef> archivedNodeRefs)
    {
        return restoreArchivedNodes(archivedNodeRefs, null, null, null);
    }

    /**
     * @see #restoreArchivedNode(NodeRef, NodeRef, QName, QName)
     */
    public List<RestoreNodeReport> restoreArchivedNodes(
            List<NodeRef> archivedNodeRefs,
            NodeRef destinationNodeRef,
            QName assocTypeQName,
            QName assocQName)
    {
        List<RestoreNodeReport> results = new ArrayList<RestoreNodeReport>(archivedNodeRefs.size());
        for (NodeRef nodeRef : archivedNodeRefs)
        {
            RestoreNodeReport result = restoreArchivedNode(nodeRef, destinationNodeRef, assocTypeQName, assocQName);
            results.add(result);
        }
        return results;
    }

    /**
     * @see #restoreAllArchivedNodes(StoreRef, NodeRef, QName, QName)
     */
    public List<RestoreNodeReport> restoreAllArchivedNodes(StoreRef originalStoreRef)
    {
        return restoreAllArchivedNodes(originalStoreRef, null, null, null);
    }

    /**
     * Finds the archive location for nodes that were deleted from the given store
     * and attempt to restore each node.
     * 
     * @see NodeService#getStoreArchiveNode(StoreRef)
     * @see #restoreArchivedNode(NodeRef, NodeRef, QName, QName)
     */
    public List<RestoreNodeReport> restoreAllArchivedNodes(
            StoreRef originalStoreRef,
            NodeRef destinationNodeRef,
            QName assocTypeQName,
            QName assocQName)
    {
        List<RestoreNodeReport> results = new ArrayList<RestoreNodeReport>(1000);
        // get all archived children using a search
        ResultSet rs = getArchivedNodes(originalStoreRef);
        try {
            // loop through the resultset and attempt to restore all the nodes
            for (ResultSetRow row : rs)
            {
                NodeRef archivedNodeRef = row.getNodeRef();
                RestoreNodeReport result = restoreArchivedNode(archivedNodeRef, destinationNodeRef, assocTypeQName, assocQName);
                results.add(result);
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
                logger.error("Closed resultSet");
            }
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Restored " + results.size() + " nodes into store " + originalStoreRef);
        }
        return results;
    }

    /**
     * This is the primary purge methd that all purge methods fall back on.  It isolates the delete
     * work in a new transaction.
     */
    public boolean purgeArchivedNode(final NodeRef archivedNodeRef)
    {
        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        RetryingTransactionCallback<Boolean> deleteCallback = new RetryingTransactionCallback<Boolean>()
        {
            public Boolean execute() throws Exception
            {
                try
                {
                    BeanHelper.getDocumentLogService().addDeletedObjectLog(archivedNodeRef, "applog_delete_done");
                    nodeService.deleteNode(archivedNodeRef);
                    return true;
                }
                catch (InvalidNodeRefException nodeRefEx)
                {
                    // not error, node has already been deleted
                    return true;
                }
                catch (Exception e)
                {
                    logger.error("Deleting node from trashcan failed: ", e);
                    return false;
                }
            }
        };
        return txnHelper.doInTransaction(deleteCallback, false, true);
    }

    /**
     * @see #purgeArchivedNode(NodeRef)
     */
    public int purgeArchivedNodes(List<NodeRef> archivedNodes)
    {
        int succeeded = 0;
        for (NodeRef archivedNodeRef : archivedNodes)
        {
            boolean success = purgeArchivedNode(archivedNodeRef);
            succeeded += success ? 1 : 0;
        }
        return succeeded;
    }

    public void purgeAllArchivedNodes(StoreRef originalStoreRef)
    {
        List<RestoreNodeReport> results = new ArrayList<RestoreNodeReport>(1000);
        // get all archived children using a search
        ResultSet rs = getArchivedNodes(originalStoreRef);
        try {
            // loop through the resultset and attempt to restore all the nodes
            for (ResultSetRow row : rs)
            {
                NodeRef archivedNodeRef = row.getNodeRef();
                purgeArchivedNode(archivedNodeRef);
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
                logger.error("Closed resultSet");
            }
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Deleted " + results.size() + " nodes originally in store " + originalStoreRef);
        }
    }

}