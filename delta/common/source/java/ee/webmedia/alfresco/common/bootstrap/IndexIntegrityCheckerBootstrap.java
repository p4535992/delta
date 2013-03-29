package ee.webmedia.alfresco.common.bootstrap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.faces.event.ActionEvent;
import javax.sql.DataSource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.Indexer;
import org.alfresco.repo.search.impl.lucene.LuceneIndexerAndSearcher;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Check that all nodes are up-to-date in lucene index and reindex if necessary.
 * 
 * @author Alar Kvell
 */
public class IndexIntegrityCheckerBootstrap {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(IndexIntegrityCheckerBootstrap.class);

    private NodeService nodeService;
    private NamespaceService namespaceService;
    private TransactionService transactionService;
    private SimpleJdbcTemplate jdbcTemplate;
    private LuceneIndexerAndSearcher indexerAndSearcher;
    private int maxTransactionsPerLuceneCommit;

    public void execute(ActionEvent event) {
        String storeRefString = ActionUtil.getParam(event, "storeRef", (String) null);
        StoreRef storeRef = storeRefString == null ? null : new StoreRef(storeRefString);
        execute(false, storeRef);
    }

    public void executeWithReindex(ActionEvent event) {
        String storeRefString = ActionUtil.getParam(event, "storeRef", (String) null);
        StoreRef storeRef = storeRefString == null ? null : new StoreRef(storeRefString);
        execute(true, storeRef);
    }

    public synchronized void execute(boolean reindexMissingNodes, final StoreRef limitStoreRef) {
        Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>> nodesToReindex;
        try {
            nodesToReindex = transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>>>() {
                @Override
                public Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>> execute() throws Throwable {
                    return checkIndexIntegrityImpl(limitStoreRef);
                }
            }, true);
        } catch (Exception e) {
            LOG.error("Error checking index integrity", e);
            return;
        }
        if (nodesToReindex.isEmpty()) {
            LOG.info("Nothing to reindex, all is OK");
            return;
        }
        try {
            reindexImpl(nodesToReindex, reindexMissingNodes);
        } catch (Exception e) {
            LOG.error("Error reindexing", e);
            return;
        }
        if (reindexMissingNodes) {
            try {
                transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>>>() {
                    @Override
                    public Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>> execute() throws Throwable {
                        return checkIndexIntegrityImpl(limitStoreRef);
                    }
                }, true);
            } catch (Exception e) {
                LOG.error("Error checking index integrity", e);
                return;
            }
        }
    }

    private Map<StoreRef, Pair<Set<NodeRef> /* updateNodes */, Set<NodeRef> /* deleteNodes */>> checkIndexIntegrityImpl(StoreRef limitStoreRef) {
        LOG.info("Querying all nodes from database (" + (limitStoreRef != null ? ("only " + limitStoreRef) : ("all except " + RetryingTransactionHelper.version2StoreRef)) + ")...");
        final AtomicInteger nodeCount = new AtomicInteger();
        final Map<StoreRef, Map<String /* changeTxnId */, List<NodeRef>>> results = new HashMap<StoreRef, Map<String, List<NodeRef>>>();
        final Map<StoreRef, Set<NodeRef>> nodeRefsByStoreRef = new HashMap<StoreRef, Set<NodeRef>>();
        jdbcTemplate.query("SELECT alf_store.protocol, alf_store.identifier, alf_node.uuid, alf_transaction.change_txn_id " +
                "FROM alf_node " +
                "LEFT JOIN alf_transaction ON alf_node.transaction_id = alf_transaction.id " +
                "LEFT JOIN alf_store ON alf_node.store_id = alf_store.id " +
                "WHERE alf_node.node_deleted = false AND " +
                (limitStoreRef == null ? "NOT " : "") +
                "(alf_store.protocol = ? AND alf_store.identifier = ?)",
                new ParameterizedRowMapper<NodeRef>() {
                    @Override
                    public NodeRef mapRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
                        StoreRef storeRef = new StoreRef(rs.getString("protocol"), rs.getString("identifier"));
                        NodeRef nodeRef = new NodeRef(storeRef, rs.getString("uuid"));
                        String changeTxnId = rs.getString("change_txn_id");
                        Map<String, List<NodeRef>> resultsByStoreRef = results.get(storeRef);
                        if (resultsByStoreRef == null) {
                            resultsByStoreRef = new HashMap<String, List<NodeRef>>();
                            results.put(storeRef, resultsByStoreRef);
                        }
                        List<NodeRef> resultsByChangeTxnId = resultsByStoreRef.get(changeTxnId);
                        if (resultsByChangeTxnId == null) {
                            resultsByChangeTxnId = new ArrayList<NodeRef>();
                            resultsByStoreRef.put(changeTxnId, resultsByChangeTxnId);
                        }
                        resultsByChangeTxnId.add(nodeRef);
                        Set<NodeRef> nodeRefs = nodeRefsByStoreRef.get(storeRef);
                        if (nodeRefs == null) {
                            nodeRefs = new HashSet<NodeRef>();
                            nodeRefsByStoreRef.put(storeRef, nodeRefs);
                        }
                        nodeRefs.add(nodeRef);
                        nodeCount.addAndGet(1);
                        return null;
                    }
                },
                (limitStoreRef != null ? limitStoreRef.getProtocol() : RetryingTransactionHelper.version2StoreRef.getProtocol()),
                (limitStoreRef != null ? limitStoreRef.getIdentifier() : RetryingTransactionHelper.version2StoreRef.getIdentifier()));
        LOG.info("Found " + nodeCount.get() + " nodes from database ("
                + (limitStoreRef != null ? ("only " + limitStoreRef) : ("all except " + RetryingTransactionHelper.version2StoreRef))
                + "). Querying index for each transaction (or node, where transaction is not present)...");
        Map<StoreRef, Pair<Set<NodeRef> /* updateNodes */, Set<NodeRef> /* deleteNodes */>> nodesToReindex = new HashMap<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>>();
        ProgressTracker progress = new ProgressTracker(nodeCount.get(), 0);
        int local = 0;
        int txGood = 0, txBad = 0;
        int nodesInTxGood = 0, nodesInTxTotal = 0;
        int nodesInTxBadPresent = 0, nodesInTxBadTotal = 0;
        int nodesSeparateGood = 0, nodesSeparateTotal = 0;
        for (Entry<StoreRef, Map<String, List<NodeRef>>> entry : results.entrySet()) {
            StoreRef storeRef = entry.getKey();
            for (Entry<String, List<NodeRef>> entry2 : entry.getValue().entrySet()) {
                String changeTxnId = entry2.getKey();
                List<NodeRef> dbNodeRefs = entry2.getValue();
                if (StringUtils.isNotBlank(changeTxnId)) {
                    local += dbNodeRefs.size();
                    ResultSet resultSet = BeanHelper.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, "TX:\"" + changeTxnId + "\"");
                    boolean good = true;
                    try {
                        List<NodeRef> luceneNodeRefs = resultSet.getNodeRefs();
                        List<NodeRef> luceneHasMoreNodeRefs = new ArrayList<NodeRef>(luceneNodeRefs);
                        luceneHasMoreNodeRefs.removeAll(dbNodeRefs);
                        for (Iterator<NodeRef> i = luceneHasMoreNodeRefs.iterator(); i.hasNext();) {
                            NodeRef luceneHasMoreNodeRef = i.next();
                            if (limitStoreRef != null && !limitStoreRef.equals(luceneHasMoreNodeRef.getStoreRef())) {
                                i.remove();
                            } else if (limitStoreRef == null && RetryingTransactionHelper.version2StoreRef.equals(luceneHasMoreNodeRef.getStoreRef())) {
                                i.remove();
                            }
                        }
                        int dbNodeRefsSize = dbNodeRefs.size();
                        if (!luceneHasMoreNodeRefs.isEmpty()) {
                            LOG.info("TX:" + changeTxnId + " has " + luceneHasMoreNodeRefs.size() + " more nodeRefs in lucene [" + luceneNodeRefs.size()
                                    + "], that are not in database ["
                                    + dbNodeRefsSize + "]: " + luceneHasMoreNodeRefs);
                            good = false;

                            Pair<Set<NodeRef>, Set<NodeRef>> nodesToReindexInStore = nodesToReindex.get(storeRef);
                            if (nodesToReindexInStore == null) {
                                nodesToReindexInStore = Pair.newInstance((Set<NodeRef>) new HashSet<NodeRef>(), (Set<NodeRef>) new HashSet<NodeRef>());
                                nodesToReindex.put(storeRef, nodesToReindexInStore);
                            }

                            Set<NodeRef> luceneHasMoreNodeRefsThatAreDeleted = new HashSet<NodeRef>(luceneHasMoreNodeRefs);
                            luceneHasMoreNodeRefsThatAreDeleted.removeAll(nodeRefsByStoreRef.get(storeRef));
                            LOG.info("  Out of those nodes missing from this TX, " + luceneHasMoreNodeRefsThatAreDeleted.size() + " do not exist in database"
                                    + (luceneHasMoreNodeRefsThatAreDeleted.isEmpty() ? "" : ", adding them to index delete queue: " + luceneHasMoreNodeRefsThatAreDeleted));
                            nodesToReindexInStore.getSecond().addAll(luceneHasMoreNodeRefsThatAreDeleted);

                            Set<NodeRef> luceneHasMoreNodeRefsThatExist = new HashSet<NodeRef>(luceneHasMoreNodeRefs);
                            luceneHasMoreNodeRefsThatExist.retainAll(nodeRefsByStoreRef.get(storeRef));
                            LOG.info("  Out of those nodes missing from this TX, " + luceneHasMoreNodeRefsThatExist.size() + " exist in database"
                                    + (luceneHasMoreNodeRefsThatExist.isEmpty() ? "" : ", adding them to index update queue: " + luceneHasMoreNodeRefsThatExist));
                            nodesToReindexInStore.getFirst().addAll(luceneHasMoreNodeRefsThatExist);
                        }
                        dbNodeRefs.removeAll(luceneNodeRefs);
                        if (!dbNodeRefs.isEmpty()) {
                            LOG.warn("TX:" + changeTxnId + " has " + dbNodeRefs.size() + " more nodeRefs in database [" + dbNodeRefsSize + "], that are not in lucene ["
                                    + luceneNodeRefs.size() + "], adding them to index update queue: " + dbNodeRefs);
                            good = false;

                            Pair<Set<NodeRef>, Set<NodeRef>> nodesToReindexInStore = nodesToReindex.get(storeRef);
                            if (nodesToReindexInStore == null) {
                                nodesToReindexInStore = Pair.newInstance((Set<NodeRef>) new HashSet<NodeRef>(), (Set<NodeRef>) new HashSet<NodeRef>());
                                nodesToReindex.put(storeRef, nodesToReindexInStore);
                            }
                            nodesToReindexInStore.getFirst().addAll(dbNodeRefs);

                            for (NodeRef dbNodeRef : dbNodeRefs) {
                                ResultSet resultSet2 = BeanHelper.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, "ID:\"" + dbNodeRef.getId() + "\"");
                                try {
                                    List<NodeRef> luceneNodeRefs2 = resultSet2.getNodeRefs();
                                    if (luceneNodeRefs2.size() != 1 || dbNodeRef.equals(luceneNodeRefs2.get(0))) {
                                        String nodeType;
                                        if (nodeService.exists(dbNodeRef)) {
                                            nodeType = nodeService.getType(dbNodeRef).toPrefixString(namespaceService);
                                        } else {
                                            nodeType = "unknown, node does not exist";
                                        }
                                        LOG.warn("NodeRef " + dbNodeRef + " supposed to be in TX:" + changeTxnId + " but wasn't, returned from lucene: " + luceneNodeRefs2
                                                + ", node type " + nodeType);
                                    } else {
                                        nodesInTxBadPresent++;
                                    }
                                    nodesInTxBadTotal++;
                                } finally {
                                    resultSet2.close();
                                }
                            }
                        }
                        nodesInTxGood += (dbNodeRefsSize - dbNodeRefs.size());
                        nodesInTxTotal += dbNodeRefsSize;
                    } finally {
                        resultSet.close();
                    }
                    if (good) {
                        txGood++;
                    } else {
                        txBad++;
                    }
                    if (local > 200) {
                        String info = progress.step(local);
                        local = 0;
                        if (info != null) {
                            LOG.info("Index checking: " + info);
                        }
                    }
                } else {
                    for (NodeRef dbNodeRef : dbNodeRefs) {
                        ResultSet resultSet = BeanHelper.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, "ID:\"" + dbNodeRef.getId() + "\"");
                        try {
                            List<NodeRef> luceneNodeRefs = resultSet.getNodeRefs();
                            if (luceneNodeRefs.size() != 1 || dbNodeRef.equals(luceneNodeRefs.get(0))) {
                                LOG.warn("NodeRef " + dbNodeRef + " returned from lucene: " + luceneNodeRefs);
                            } else {
                                nodesSeparateGood++;
                            }
                            nodesSeparateTotal++;
                        } finally {
                            resultSet.close();
                        }
                        local++;
                        if (local > 200) {
                            String info = progress.step(local);
                            local = 0;
                            if (info != null) {
                                LOG.info("Index checking: " + info);
                            }
                        }
                    }
                }
            }
        }
        String info = progress.step(local);
        if (info != null) {
            LOG.info("Index checking: " + info);
        }
        LOG.info("Finished querying index. " + txGood + " transactions were OK, " + txBad + " transactions were inconsistent. "
                + nodesInTxGood + " out of " + nodesInTxTotal + " nodes were present in correct transaction. "
                + nodesInTxBadPresent + " out of " + nodesInTxBadTotal + " nodes-not-in-correct-transaction were present in index. " +
                +nodesSeparateGood + " out of " + nodesSeparateTotal + " nodes-not-in-transaction were present in index.");
        return nodesToReindex;
    }

    private void reindexImpl(Map<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>> nodesToReindex, boolean reindexMissingNodes) {
        final RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
        for (Entry<StoreRef, Pair<Set<NodeRef>, Set<NodeRef>>> entry : nodesToReindex.entrySet()) {
            final StoreRef storeRef = entry.getKey();
            final Set<NodeRef> nodesToUpdate = entry.getValue().getFirst();
            final Set<NodeRef> nodesToDelete = entry.getValue().getSecond();
            LOG.info("Index for store " + storeRef + " - reindexing needs to update " + nodesToUpdate.size() + " nodes and delete " + nodesToDelete.size() + " nodes");
            if (!reindexMissingNodes) {
                LOG.info("Skipping reindexing");
                continue;
            }
            ProgressTracker progress = new ProgressTracker(nodesToUpdate.size() + nodesToDelete.size(), 0);
            while (!nodesToUpdate.isEmpty() || !nodesToDelete.isEmpty()) {
                Integer countCompleted = txHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {
                    @Override
                    public Integer execute() throws Throwable {
                        Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                        int count = 0;
                        for (Iterator<NodeRef> i = nodesToUpdate.iterator(); i.hasNext() && count < (maxTransactionsPerLuceneCommit * 3);) {
                            NodeRef nodeRef = i.next();
                            if (nodeService.exists(nodeRef)) {
                                indexer.updateNode(nodeRef);
                            } else {
                                // only the child node ref is relevant
                                ChildAssociationRef assocRef = new ChildAssociationRef(
                                        ContentModel.ASSOC_CHILDREN,
                                        null,
                                        null,
                                        nodeRef);
                                indexer.deleteNode(assocRef);
                            }
                            i.remove();
                            count++;
                        }
                        for (Iterator<NodeRef> i = nodesToDelete.iterator(); i.hasNext() && count < (maxTransactionsPerLuceneCommit * 3);) {
                            NodeRef nodeRef = i.next();
                            // only the child node ref is relevant
                            ChildAssociationRef assocRef = new ChildAssociationRef(
                                    ContentModel.ASSOC_CHILDREN,
                                    null,
                                    null,
                                    nodeRef);
                            indexer.deleteNode(assocRef);
                            i.remove();
                            count++;
                        }
                        return count;
                    }
                });
                String info = progress.step(countCompleted);
                if (info != null) {
                    LOG.info("Reindexing: " + info);
                }
            }
            LOG.info("Index for store " + storeRef + " - reindexing completed");
        }
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setIndexerAndSearcher(LuceneIndexerAndSearcher indexerAndSearcher) {
        this.indexerAndSearcher = indexerAndSearcher;
    }

    public void setTransactionIntegrityCheckerEnabled(boolean transactionIntegrityCheckerEnabled) {
        RetryingTransactionHelper.transactionIntegrityCheckerEnabled = transactionIntegrityCheckerEnabled;
    }

    public void setMaxTransactionsPerLuceneCommit(int maxTransactionsPerLuceneCommit) {
        this.maxTransactionsPerLuceneCommit = maxTransactionsPerLuceneCommit;
    }

}
