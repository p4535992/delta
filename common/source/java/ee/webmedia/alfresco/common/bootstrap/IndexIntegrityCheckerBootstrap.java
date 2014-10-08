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
import java.util.concurrent.atomic.AtomicReference;

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
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Check that all nodes are up-to-date in lucene index and reindex if necessary.
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
        Map<StoreRef, Set<NodeRef>> nodesToReindex;
        try {
            nodesToReindex = transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Map<StoreRef, Set<NodeRef>>>() {
                @Override
                public Map<StoreRef, Set<NodeRef>> execute() throws Throwable {
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
    }

    private Map<StoreRef, Set<NodeRef>> checkIndexIntegrityImpl(final StoreRef limitStoreRef) {
        LOG.info("Querying all nodes from database (" + (limitStoreRef != null ? ("only " + limitStoreRef) : ("all except " + RetryingTransactionHelper.version2StoreRef)) + ")...");
        final AtomicInteger localNodeCount = new AtomicInteger();
        final Map<StoreRef, Set<NodeRef>> nodesToReindex = new HashMap<StoreRef, Set<NodeRef>>();
        final Map<StoreRef, Set<NodeRef>> currentTxnNodes = new HashMap<StoreRef, Set<NodeRef>>();
        final AtomicReference<String> currentChangeTxnId = new AtomicReference<String>();

        final AtomicInteger txGood = new AtomicInteger();
        final AtomicInteger txBad = new AtomicInteger();
        final AtomicInteger nodesInTxGood = new AtomicInteger();
        final AtomicInteger nodesInTxTotal = new AtomicInteger();
        final AtomicInteger nodesInTxBadPresent = new AtomicInteger();
        final AtomicInteger nodesInTxBadTotal = new AtomicInteger();
        final AtomicInteger nodesSeparateGood = new AtomicInteger();
        final AtomicInteger nodesSeparateTotal = new AtomicInteger();

        long totalNodeCount = jdbcTemplate.queryForLong("SELECT COUNT(*) " +
                "FROM alf_node " +
                "LEFT JOIN alf_store ON alf_node.store_id = alf_store.id " +
                "WHERE alf_node.node_deleted = false AND " +
                (limitStoreRef == null ? "NOT " : "") +
                "(alf_store.protocol = ? AND alf_store.identifier = ?)",
                (limitStoreRef != null ? limitStoreRef.getProtocol() : RetryingTransactionHelper.version2StoreRef.getProtocol()),
                (limitStoreRef != null ? limitStoreRef.getIdentifier() : RetryingTransactionHelper.version2StoreRef.getIdentifier()));
        LOG.info("Found " + totalNodeCount + " nodes from database ("
                + (limitStoreRef != null ? ("only " + limitStoreRef) : ("all except " + RetryingTransactionHelper.version2StoreRef))
                + ")." + (totalNodeCount > 0 ? " Querying index for each transaction (or node, where transaction is not present)..." : ""));
        if (totalNodeCount <= 0) {
            return nodesToReindex;
        }

        String query = "SELECT alf_store.protocol, alf_store.identifier, alf_node.uuid, alf_transaction.change_txn_id " +
                "FROM alf_node " +
                "LEFT JOIN alf_transaction ON alf_node.transaction_id = alf_transaction.id " +
                "LEFT JOIN alf_store ON alf_node.store_id = alf_store.id " +
                "WHERE alf_node.node_deleted = false AND " +
                (limitStoreRef == null ? "NOT " : "") +
                "(alf_store.protocol = ? AND alf_store.identifier = ?) " +
                "ORDER BY alf_transaction.change_txn_id ASC";
        Object[] args = new Object[] {
                (limitStoreRef != null ? limitStoreRef.getProtocol() : RetryingTransactionHelper.version2StoreRef.getProtocol()),
                (limitStoreRef != null ? limitStoreRef.getIdentifier() : RetryingTransactionHelper.version2StoreRef.getIdentifier())
        };
        final ProgressTracker progress = new ProgressTracker(totalNodeCount, 0);
        jdbcTemplate.getJdbcOperations().query(query, args, new RowCallbackHandler() {
            @Override
            public void processRow(java.sql.ResultSet rs) throws SQLException {
                StoreRef storeRef = new StoreRef(rs.getString("protocol"), rs.getString("identifier"));
                NodeRef nodeRef = new NodeRef(storeRef, rs.getString("uuid"));
                String changeTxnId = rs.getString("change_txn_id");

                if (currentChangeTxnId.get() == null || !currentChangeTxnId.get().equals(changeTxnId)) {
                    check(currentTxnNodes, currentChangeTxnId.get(), limitStoreRef, nodesToReindex, txGood, txBad, nodesInTxGood, nodesInTxTotal, nodesInTxBadPresent,
                            nodesInTxBadTotal, nodesSeparateGood, nodesSeparateTotal);
                    currentTxnNodes.clear();
                    currentChangeTxnId.set(changeTxnId);
                }

                Set<NodeRef> currentTxnNodesByStoreRef = currentTxnNodes.get(storeRef);
                if (currentTxnNodesByStoreRef == null) {
                    currentTxnNodesByStoreRef = new HashSet<NodeRef>();
                    currentTxnNodes.put(storeRef, currentTxnNodesByStoreRef);
                }
                currentTxnNodesByStoreRef.add(nodeRef);
                localNodeCount.incrementAndGet();
                if (localNodeCount.get() > 200) {
                    String info = progress.step(localNodeCount.get());
                    localNodeCount.set(0);
                    if (info != null) {
                        LOG.info("Index checking: " + info);
                    }
                }
            }
        });
        // Process last row
        check(currentTxnNodes, currentChangeTxnId.get(), limitStoreRef, nodesToReindex, txGood, txBad, nodesInTxGood, nodesInTxTotal, nodesInTxBadPresent, nodesInTxBadTotal,
                nodesSeparateGood, nodesSeparateTotal);
        String info = progress.step(localNodeCount.get());
        if (info != null) {
            LOG.info("Index checking: " + info);
        }
        LOG.info("Finished querying index. " + txGood + " transactions were OK, " + txBad + " transactions were inconsistent. "
                + nodesInTxGood + " out of " + nodesInTxTotal + " nodes were present in correct transaction. "
                + nodesInTxBadPresent + " out of " + nodesInTxBadTotal + " nodes-not-in-correct-transaction were present in index. "
                + nodesSeparateGood + " out of " + nodesSeparateTotal + " nodes-not-in-transaction were present in index.");
        return nodesToReindex;
    }

    private void check(Map<StoreRef, Set<NodeRef>> nodes, String changeTxnId, StoreRef limitStoreRef, Map<StoreRef, Set<NodeRef>> nodesToReindex,
            AtomicInteger txGood, AtomicInteger txBad, AtomicInteger nodesInTxGood, AtomicInteger nodesInTxTotal,
            AtomicInteger nodesInTxBadPresent, AtomicInteger nodesInTxBadTotal, AtomicInteger nodesSeparateGood, AtomicInteger nodesSeparateTotal) {

        for (Entry<StoreRef, Set<NodeRef>> entry : nodes.entrySet()) {
            StoreRef storeRef = entry.getKey();
            Set<NodeRef> dbNodeRefs = entry.getValue();
            if (StringUtils.isNotBlank(changeTxnId)) {
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
                                + dbNodeRefsSize + "], adding them to index update/delete queue: " + luceneHasMoreNodeRefs);
                        good = false;

                        Set<NodeRef> nodesToReindexInStore = nodesToReindex.get(storeRef);
                        if (nodesToReindexInStore == null) {
                            nodesToReindexInStore = new HashSet<NodeRef>();
                            nodesToReindex.put(storeRef, nodesToReindexInStore);
                        }
                        nodesToReindexInStore.addAll(luceneHasMoreNodeRefs);
                    }
                    dbNodeRefs.removeAll(luceneNodeRefs);
                    if (!dbNodeRefs.isEmpty()) {
                        LOG.warn("TX:" + changeTxnId + " has " + dbNodeRefs.size() + " more nodeRefs in database [" + dbNodeRefsSize + "], that are not in lucene ["
                                + luceneNodeRefs.size() + "], adding them to index update queue: " + dbNodeRefs);
                        good = false;

                        Set<NodeRef> nodesToReindexInStore = nodesToReindex.get(storeRef);
                        if (nodesToReindexInStore == null) {
                            nodesToReindexInStore = new HashSet<NodeRef>();
                            nodesToReindex.put(storeRef, nodesToReindexInStore);
                        }
                        nodesToReindexInStore.addAll(dbNodeRefs);

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
                                    nodesInTxBadPresent.incrementAndGet();
                                }
                                nodesInTxBadTotal.incrementAndGet();
                            } finally {
                                resultSet2.close();
                            }
                        }
                    }
                    nodesInTxGood.addAndGet(dbNodeRefsSize - dbNodeRefs.size());
                    nodesInTxTotal.addAndGet(dbNodeRefsSize);
                } finally {
                    resultSet.close();
                }
                if (good) {
                    txGood.incrementAndGet();
                } else {
                    txBad.incrementAndGet();
                }
            } else {
                for (NodeRef dbNodeRef : dbNodeRefs) {
                    ResultSet resultSet = BeanHelper.getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, "ID:\"" + dbNodeRef.getId() + "\"");
                    try {
                        List<NodeRef> luceneNodeRefs = resultSet.getNodeRefs();
                        if (luceneNodeRefs.size() != 1 || !dbNodeRef.equals(luceneNodeRefs.get(0))) {
                            LOG.warn("NodeRef in database " + dbNodeRef + " but in lucene: " + luceneNodeRefs);
                            Set<NodeRef> nodesToReindexInStore = nodesToReindex.get(storeRef);
                            if (nodesToReindexInStore == null) {
                                nodesToReindexInStore = new HashSet<NodeRef>();
                                nodesToReindex.put(storeRef, nodesToReindexInStore);
                            }
                            nodesToReindexInStore.add(dbNodeRef);
                        } else {
                            nodesSeparateGood.incrementAndGet();
                        }
                        nodesSeparateTotal.incrementAndGet();
                    } finally {
                        resultSet.close();
                    }
                }
            }
        }
    }

    private void reindexImpl(Map<StoreRef, Set<NodeRef>> nodesToReindex, boolean reindexMissingNodes) {
        final RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
        for (Entry<StoreRef, Set<NodeRef>> entry : nodesToReindex.entrySet()) {
            final StoreRef storeRef = entry.getKey();
            final Set<NodeRef> nodes = entry.getValue();
            LOG.info("Index for store " + storeRef + " - reindexing needs to update or delete " + nodes.size() + " nodes");
            if (!reindexMissingNodes || nodes.isEmpty()) {
                LOG.info("Skipping reindexing");
                continue;
            }
            ProgressTracker progress = new ProgressTracker(nodes.size(), 0);
            while (!nodes.isEmpty()) {
                final AtomicInteger countCompleted = new AtomicInteger();
                try {
                    txHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {
                        @Override
                        public Integer execute() throws Throwable {
                            Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                            for (Iterator<NodeRef> i = nodes.iterator(); i.hasNext() && countCompleted.get() < (maxTransactionsPerLuceneCommit * 3);) {
                                NodeRef nodeRef = i.next();
                                i.remove();
                                countCompleted.incrementAndGet();
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
                            }
                            return null;
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Error reindexing " + countCompleted.get() + " nodes, continuing with next batch", e);
                }
                String info = progress.step(countCompleted.get());
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
