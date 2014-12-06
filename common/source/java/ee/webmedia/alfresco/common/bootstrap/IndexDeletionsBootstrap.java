package ee.webmedia.alfresco.common.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.Indexer;
import org.alfresco.repo.search.impl.lucene.AbstractLuceneBase;
import org.alfresco.repo.search.impl.lucene.LuceneIndexerAndSearcher;
import org.alfresco.repo.search.impl.lucene.index.IndexInfo;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.AbstractLifecycleBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEvent;

import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * If file lucene-indexes/IndexDeletions exists, delete nodes specified in that file (each noderef on a separate line) from lucene index.
 */
public class IndexDeletionsBootstrap extends AbstractLifecycleBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(IndexDeletionsBootstrap.class);

    private static final String INDEX_DELETIONS_FILENAME = "IndexDeletions";

    private LuceneIndexerAndSearcher indexerAndSearcher;
    private RetryingTransactionHelper txHelper;

    @Override
    protected void onBootstrap(ApplicationEvent event) {
        final String indexRootLocation = indexerAndSearcher.getIndexRootLocation();
        final File file = new File(indexRootLocation, INDEX_DELETIONS_FILENAME);
        if (!file.exists()) {
            return;
        }
        final Map<StoreRef, Set<NodeRef>> nodesToDelete = GetNodesToDelete(file);

        if (nodesToDelete != null) {
            deleteNodesFromIndex(nodesToDelete);
            runMerge(nodesToDelete.keySet());
        }

        LOG.info("Deleting file " + file);
        if (!file.delete()) {
            throw new RuntimeException("Error deleting file " + file);
        }
        LOG.info("Successfully deleted file " + file);
    }

    private Map<StoreRef, Set<NodeRef>> GetNodesToDelete(File file) {
        LOG.info("Reading lines from " + file);
        List<String> lines;
        try {
            lines = FileUtils.readLines(file);
        } catch (final IOException e) {
            throw new RuntimeException("Reading lines from " + file + " failed: " + e.getMessage(), e);
        }
        LOG.info("Read " + lines.size() + " lines, parsing noderefs");
        final Map<StoreRef, Set<NodeRef>> nodesToDelete = new HashMap<StoreRef, Set<NodeRef>>();
        for (String line : lines) {
            line = line.trim();
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            final NodeRef nodeRef = new NodeRef(line);
            Set<NodeRef> set = nodesToDelete.get(nodeRef.getStoreRef());
            if (set == null) {
                set = new HashSet<NodeRef>();
                nodesToDelete.put(nodeRef.getStoreRef(), set);
            }
            set.add(nodeRef);
        }
        long count = 0;
        for (final Set<NodeRef> set : nodesToDelete.values()) {
            count += set.size();
        }
        LOG.info("Parsed " + count + " unique noderefs");
        if (count == 0) {
            return null;
        }
        return nodesToDelete;
    }

    @Override
    protected void onShutdown(ApplicationEvent event) {
    }

    private void deleteNodesFromIndex(Map<StoreRef, Set<NodeRef>> nodesToDelete) {
        for (final Entry<StoreRef, Set<NodeRef>> entry : nodesToDelete.entrySet()) {
            final StoreRef storeRef = entry.getKey();
            final Set<NodeRef> nodes = entry.getValue();
            if (nodes.isEmpty()) {
                continue;
            }
            LOG.info("Index for store " + storeRef + " - need to delete " + nodes.size() + " nodes");
            final ProgressTracker progress = new ProgressTracker(nodes.size(), 0);
            while (!nodes.isEmpty()) {
                final AtomicInteger countCompleted = new AtomicInteger();
                try {
                    txHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {
                        @Override
                        public Integer execute() throws Throwable {
                            final Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                            for (final Iterator<NodeRef> i = nodes.iterator(); i.hasNext() && countCompleted.get() < 10000;) {
                                final NodeRef nodeRef = i.next();
                                i.remove();
                                countCompleted.incrementAndGet();
                                // only the child node ref is relevant
                                final ChildAssociationRef assocRef = new ChildAssociationRef(
                                        ContentModel.ASSOC_CHILDREN,
                                        null,
                                        null,
                                        nodeRef);
                                indexer.deleteNode(assocRef);
                            }
                            return null;
                        }
                    });
                } catch (final Exception e) {
                    LOG.error("Error deleting " + countCompleted.get() + " nodes from index, continuing with next batch", e);
                }
                final String info = progress.step(countCompleted.get());
                if (info != null) {
                    LOG.info("Deleting from index: " + info);
                }
            }
            LOG.info("Index for store " + storeRef + " - deleting completed");
        }
    }

    private void runMerge(Set<StoreRef> stores) {
        for (final StoreRef storeRef : stores) {
            try {
                txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        final Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                        final Field indexInfoField = AbstractLuceneBase.class.getDeclaredField("indexInfo");
                        indexInfoField.setAccessible(true);
                        final IndexInfo indexInfo = (IndexInfo) indexInfoField.get(indexer);
                        LOG.info("Starting special merge on indexInfo: " + indexInfo.toString() + indexInfo.dumpInfoAsString());
                        indexInfo.runMergeNow();
                        LOG.info("Completed special merge on indexInfo: " + indexInfo.toString() + indexInfo.dumpInfoAsString());
                        return null;
                    }
                });
            } catch (final Exception e) {
                LOG.error("Error running special merge on store " + storeRef + ", continuing with next store (if available)", e);
            }
        }
    }

    public void setIndexerAndSearcher(LuceneIndexerAndSearcher indexerAndSearcher) {
        this.indexerAndSearcher = indexerAndSearcher;
    }

    public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
        txHelper = retryingTransactionHelper;
    }
}
