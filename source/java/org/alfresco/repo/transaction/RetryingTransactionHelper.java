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
package org.alfresco.repo.transaction;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeDaoService;

import java.lang.reflect.Method;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import net.sf.ehcache.distribution.RemoteCacheException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.error.ExceptionStackUtil;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.search.impl.lucene.LuceneResultSetRow;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Fieldable;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.cache.CacheException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.ibatis.common.jdbc.exception.NestedSQLException;

import ee.webmedia.alfresco.common.bootstrap.IndexIntegrityCheckerBootstrap;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.common.service.CustomReindexComponent;
import ee.webmedia.alfresco.common.transaction.TransactionHelperWrapperException;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.CalendarUtil;

/**
 * A helper that runs a unit of work inside a UserTransaction,
 * transparently retrying the unit of work if the cause of
 * failure is an optimistic locking or deadlock condition.
 * <p>
 * Defaults:
 * <ul>
 *   <li><b>maxRetries: 20</b></li>
 *   <li><b>minRetryWaitMs: 100</b></li>
 *   <li><b>maxRetryWaitMs: 2000</b></li>
 *   <li><b>retryWaitIncrementMs: 100</b></li>
 * </ul>
 * <p>
 * To get details of 'why' transactions are retried use the following log level:<br>
 * <b>Summary: log4j.logger.org.alfresco.repo.transaction.RetryingTransactionHelper=INFO</b><br>
 * <b>Details: log4j.logger.org.alfresco.repo.transaction.RetryingTransactionHelper=DEBUG</b><br>
 * 
 *
 * @author Derek Hulley
 */
public class RetryingTransactionHelper
{
    private static final String MSG_READ_ONLY = "permissions.err_read_only";
    private static final String KEY_ACTIVE_TRANSACTION = "RetryingTransactionHelper.ActiveTxn";
    private static Log    logger = LogFactory.getLog(RetryingTransactionHelper.class);

    /**
     * Exceptions that trigger retries.
     */
    @SuppressWarnings("unchecked")
    public static final Class[] RETRY_EXCEPTIONS;
    static
    {
        RETRY_EXCEPTIONS = new Class[] {
                ConcurrencyFailureException.class,
                DeadlockLoserDataAccessException.class,
                StaleObjectStateException.class,
                JdbcUpdateAffectedIncorrectNumberOfRowsException.class,     // Similar to StaleObjectState
                LockAcquisitionException.class,
                ConstraintViolationException.class,
                UncategorizedSQLException.class,
                SQLException.class,
                BatchUpdateException.class,
                DataIntegrityViolationException.class,
                StaleStateException.class,
                ObjectNotFoundException.class,
                CacheException.class,                       // Usually a cache replication issue
                RemoteCacheException.class,                 // A cache replication issue
                SQLGrammarException.class // Actually specific to MS SQL Server 2005 - we check for this
                };
    }

    /**
     * Reference to the TransactionService instance.
     */
    private TransactionService txnService;
    private SimpleJdbcOperations jdbcTemplate;

//    /** Performs post-failure exception neatening */
//    private ExceptionTransformer exceptionTransformer;
    /** The maximum number of retries. -1 for infinity. */
    private int maxRetries;
    /** The minimum time to wait between retries. */
    private int minRetryWaitMs;
    /** The maximum time to wait between retries. */
    private int maxRetryWaitMs;
    /** How much to increase the wait time with each retry. */
    private int retryWaitIncrementMs;
    
    public static boolean transactionIntegrityCheckerEnabled = false;
    public static boolean transactionIntegrityCheckerInMainThreadEnabled = false;
    public static ThreadLocal<LinkedList<Set<NodeRef>>> nodesUpdated = new ThreadLocal<LinkedList<Set<NodeRef>>>() {
        @Override
        protected LinkedList<Set<NodeRef>> initialValue() {
            return new LinkedList<Set<NodeRef>>();
        }
    };
    private static ThreadLocal<AtomicLong> nodesUpdatedCheckTotalMillis = new ThreadLocal<AtomicLong>() {
        @Override
        protected AtomicLong initialValue() {
            return new AtomicLong();
        }
    };
    private static ThreadLocal<AtomicLong> lastNodesUpdatedCheck = new ThreadLocal<AtomicLong>() {
        @Override
        protected AtomicLong initialValue() {
            return new AtomicLong(System.currentTimeMillis());
        }
    };
    public static final NodeRef deleteNode = new NodeRef("DELETE://DELETE/DELETE");
    public static final StoreRef version2StoreRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "version2Store");

    /**
     * Whether the the transactions may only be reads
     */
    private boolean readOnly;
    
    /**
     * Random number generator for retry delays.
     */
    private Random random;

    /**
     * Callback interface
     * @author Derek Hulley
     */
    public interface RetryingTransactionCallback<Result>
    {
        /**
         * Perform a unit of transactional work.
         *
         * @return              Return the result of the unit of work
         * @throws Throwable    This can be anything and will guarantee either a retry or a rollback
         */
        public Result execute() throws Throwable;
    };

    /**
     * Default constructor.
     */
    public RetryingTransactionHelper()
    {
        this.random = new Random(System.currentTimeMillis());
        this.maxRetries = 20;
        this.minRetryWaitMs = 100;
        this.maxRetryWaitMs = 2000;
        this.retryWaitIncrementMs = 100;
    }

    // Setters.

//    /**
//     * Optionally set the component that will transform or neaten any exceptions that are
//     * propagated.
//     */
//    public void setExceptionTransformer(ExceptionTransformer exceptionTransformer)
//    {
//        this.exceptionTransformer = exceptionTransformer;
//    }
//
    /**
     * Set the TransactionService.
     */
    public void setTransactionService(TransactionService service)
    {
        this.txnService = service;
    }

    /**
     * Set the maximimum number of retries. -1 for infinity.
     */
    public void setMaxRetries(int maxRetries)
    {
        this.maxRetries = maxRetries;
    }

    public void setMinRetryWaitMs(int minRetryWaitMs)
    {
        this.minRetryWaitMs = minRetryWaitMs;
    }

    public void setMaxRetryWaitMs(int maxRetryWaitMs)
    {
        this.maxRetryWaitMs = maxRetryWaitMs;
    }

    public void setRetryWaitIncrementMs(int retryWaitIncrementMs)
    {
        this.retryWaitIncrementMs = retryWaitIncrementMs;
    }

    /**
     * Set whether this helper only supports read transactions.
     */
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    /**
     * Execute a callback in a transaction until it succeeds, fails
     * because of an error not the result of an optimistic locking failure,
     * or a deadlock loser failure, or until a maximum number of retries have
     * been attempted.
     * <p>
     * If there is already an active transaction, then the callback is merely
     * executed and any retry logic is left to the caller.  The transaction
     * will attempt to be read-write.
     *
     * @param cb                The callback containing the unit of work.
     * @return                  Returns the result of the unit of work.
     * @throws                  RuntimeException  all checked exceptions are converted
     */
    public <R> R doInTransaction(RetryingTransactionCallback<R> cb)
    {
        return doInTransaction(cb, false, false);
    }

    /**
     * Execute a callback in a transaction until it succeeds, fails
     * because of an error not the result of an optimistic locking failure,
     * or a deadlock loser failure, or until a maximum number of retries have
     * been attempted.
     * <p>
     * If there is already an active transaction, then the callback is merely
     * executed and any retry logic is left to the caller.
     *
     * @param cb                The callback containing the unit of work.
     * @param readOnly          Whether this is a read only transaction.
     * @return                  Returns the result of the unit of work.
     * @throws                  RuntimeException  all checked exceptions are converted
     */
    public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly)
    {
        return doInTransaction(cb, readOnly, false);
    }
    
    public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly, boolean requiresNew){
        return doInTransaction(cb, readOnly, requiresNew, false);
    }

    /**
     * Execute a callback in a transaction until it succeeds, fails
     * because of an error not the result of an optimistic locking failure,
     * or a deadlock loser failure, or until a maximum number of retries have
     * been attempted.
     * <p>
     * It is possible to force a new transaction to be created or to partake in
     * any existing transaction.
     *
     * @param cb                The callback containing the unit of work.
     * @param readOnly          Whether this is a read only transaction.
     * @param requiresNew       <tt>true</tt> to force a new transaction or
     *                          <tt>false</tt> to partake in any existing transaction.
     * @return                  Returns the result of the unit of work.
     * @throws                  RuntimeException  all checked exceptions are converted
     */
    public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly, boolean requiresNew, boolean throwOriginalException)
    {
        if (this.readOnly && !readOnly)
        {
            throw new AccessDeniedException(MSG_READ_ONLY);
        }
        // Track the last exception caught, so that we
        // can throw it if we run out of retries.
        RuntimeException lastException = null;
        for (int count = 0; count == 0 || count < maxRetries; count++)
        {
            long iterationStartTime = System.nanoTime();
            UserTransaction txn = null;
            boolean nodesUpdatedRemoveLast = false;
            try
            {
                if (requiresNew)
                {
                    txn = txnService.getNonPropagatingUserTransaction(readOnly);
                }
                else
                {
                    TxnReadState readState = AlfrescoTransactionSupport.getTransactionReadState();
                    switch (readState)
                    {
                        case TXN_READ_ONLY:
                            if (!readOnly)
                            {
                                // The current transaction is read-only, but a writable transaction is requested
                                throw new AlfrescoRuntimeException("Read-Write transaction started within read-only transaction");
                            }
                            // We are in a read-only transaction and this is what we require so continue with it.
                            break;
                        case TXN_READ_WRITE:
                            // We are in a read-write transaction.  It cannot be downgraded so just continue with it.
                            break;
                        case TXN_NONE:
                            // There is no current transaction so we need a new one.
                            txn = txnService.getUserTransaction(readOnly);
                            break;
                        default:
                            throw new RuntimeException("Unknown transaction state: " + readState);
                    }
                }
                if (txn != null)
                {
                    if (transactionIntegrityCheckerEnabled)
                    {
                        nodesUpdated.get().addLast(readOnly ? null : new HashSet<NodeRef>());
                        nodesUpdatedRemoveLast = true;
                    }
                    txn.begin();
                    // Wrap it to protect it
                    UserTransactionProtectionAdvise advise = new UserTransactionProtectionAdvise();
                    ProxyFactory proxyFactory = new ProxyFactory(txn);
                    proxyFactory.addAdvice(advise);
                    UserTransaction wrappedTxn = (UserTransaction) proxyFactory.getProxy();
                    // Store the UserTransaction for static retrieval.  There is no need to unbind it
                    // because the transaction management will do that for us.
                    AlfrescoTransactionSupport.bindResource(KEY_ACTIVE_TRANSACTION, wrappedTxn);
                }
                // Do the work.
                R result = cb.execute();
                // Only commit if we 'own' the transaction.
                if (txn != null)
                {
                    if (txn.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                    {
                        // Something caused the transaction to be marked for rollback
                        // There is no recovery or retrying with this
                        long startTime = System.nanoTime();
                        try {
                            txn.rollback();
                        } finally {
                            StatisticsPhaseListener.addTimingNano(readOnly ? StatisticsPhaseListenerLogColumn.TX_ROLLBACK_RO : StatisticsPhaseListenerLogColumn.TX_ROLLBACK_RW, startTime);
                        }
                    }
                    else
                    {
                        // The transaction hasn't been flagged for failure so the commit
                        // sould still be good.
                        long startTime = System.nanoTime();
                        try {
                            txn.commit();
                        } finally {
                            StatisticsPhaseListener.addTimingNano(readOnly ? StatisticsPhaseListenerLogColumn.TX_COMMIT_RO : StatisticsPhaseListenerLogColumn.TX_COMMIT_RW, startTime);
                        }
                        if (transactionIntegrityCheckerEnabled && !readOnly) {
                            final Set<NodeRef> nodesUpdatedLocal = nodesUpdated.get().getLast();
                            if (!nodesUpdatedLocal.isEmpty()) {
                                nodesUpdatedLocal.remove(RetryingTransactionHelper.deleteNode);
                                for (Iterator<NodeRef> i = nodesUpdatedLocal.iterator(); i.hasNext();) {
                                    NodeRef nodeRef = i.next();
                                    if (version2StoreRef.equals(nodeRef.getStoreRef())) {
                                        i.remove();
                                    }
                                }
                                
                                if (!nodesUpdatedLocal.isEmpty()) {
                                    long startNodesUpdatedCheckTime = System.nanoTime();
                                    doInTransaction(new RetryingTransactionCallback<Void>() {
                                        @Override
                                        public Void execute() throws Throwable {
                                            checkNodesUpdated(nodesUpdatedLocal);
                                            return null;
                                        }
                                    }, true, true);
                                    long stopNodesUpdatedCheckTime = System.nanoTime();
                                    long total = nodesUpdatedCheckTotalMillis.get().addAndGet(CalendarUtil.duration(startNodesUpdatedCheckTime, stopNodesUpdatedCheckTime));
                                    long time = System.currentTimeMillis();
                                    if (time > (lastNodesUpdatedCheck.get().get() + 60000L)) {
                                        lastNodesUpdatedCheck.get().set(time);
                                        logger.info("TransactionIntegrityChecker: Total time used in this thread so far " + total + " ms");
                                    }
                                }
                            }
                        }
                    }
                }
                if (logger.isDebugEnabled())
                {
                    if (count != 0)
                    {
                        logger.debug("\n" +
                                "Transaction succeeded: \n" +
                                "   Thread: " + Thread.currentThread().getName() + "\n" +
                                "   Txn:    " + txn + "\n" +
                                "   Iteration: " + count);
                    }
                }
                if (nodesUpdatedRemoveLast)
                {
                    nodesUpdated.get().removeLast();
                }
                return result;
            }
            catch (Throwable e)
            {
                if (nodesUpdatedRemoveLast)
                {
                    nodesUpdated.get().removeLast();
                }
                // Somebody else 'owns' the transaction, so just rethrow.
                if (txn == null)
                {
                    if (throwOriginalException) {
                        throw new TransactionHelperWrapperException(e);
                    }
                    RuntimeException ee = AlfrescoRuntimeException.makeRuntimeException(
                            e, "Exception from transactional callback: " + cb);
                    throw ee;
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("\n" +
                            "Transaction commit failed: \n" +
                            "   Thread: " + Thread.currentThread().getName() + "\n" +
                            "   Txn:    " + txn + "\n" +
                            "   Iteration: " + count + "\n" +
                            "   Exception follows:",
                            e);
                }
                // Rollback if we can.
                if (txn != null)
                {
                    try
                    {
                        int txnStatus = txn.getStatus();
                        // We can only rollback if a transaction was started (NOT NO_TRANSACTION) and
                        // if that transaction has not been rolled back (NOT ROLLEDBACK).
                        // If an exception occurs while the transaction is being created (e.g. no database connection)
                        // then the status will be NO_TRANSACTION.
                        if (txnStatus != Status.STATUS_NO_TRANSACTION && txnStatus != Status.STATUS_ROLLEDBACK)
                        {
                            long startTime = System.nanoTime();
                            try {
                                txn.rollback();
                            } finally {
                                StatisticsPhaseListener.addTimingNano(readOnly ? StatisticsPhaseListenerLogColumn.TX_ROLLBACK_RO : StatisticsPhaseListenerLogColumn.TX_ROLLBACK_RW, startTime);
                            }
                        }
                    }
                    catch (Throwable e1)
                    {
                        // A rollback failure should not preclude a retry, but logging of the rollback failure is required
                        logger.error("Rollback failure.  Normal retry behaviour will resume.", e1);
                    }
                }
                if (e instanceof RollbackException)
                {
                    if (e.getCause() instanceof RuntimeException)
                    {
                        lastException = (RuntimeException) e.getCause();
                    }
                    else
                    {
                        if (throwOriginalException)
                        {
                            lastException = new TransactionHelperWrapperException(e.getCause());
                        }
                        else
                        {
                            lastException = new AlfrescoRuntimeException("Exception in Transaction.", e.getCause());
                        }
                    }
                }
                else
                {
                    if (e instanceof RuntimeException)
                    {
                        lastException = (RuntimeException) e;
                    }
                    else
                    {
                        if (throwOriginalException)
                        {
                            lastException = new TransactionHelperWrapperException(e);
                        }
                        else
                        {
                            lastException = new AlfrescoRuntimeException("Exception in Transaction.", e);
                        }
                    }
                }
                // Check if there is a cause for retrying
                Throwable retryCause = extractRetryCause(e);
                if (retryCause != null)
                {
                    // Sleep a random amount of time before retrying.
                    // The sleep interval increases with the number of retries.
                    int sleepIntervalRandom = count > 0 ? random.nextInt(count * retryWaitIncrementMs) : minRetryWaitMs;
                    int sleepInterval = Math.min(maxRetryWaitMs, sleepIntervalRandom);
                    sleepInterval = Math.max(sleepInterval, minRetryWaitMs);
                    if (logger.isInfoEnabled() && !logger.isDebugEnabled())
                    {
                        String msg = String.format(
                                "Retrying %s: count %2d; wait: %1.1fs; msg: \"%s\"; exception: (%s)",
                                Thread.currentThread().getName(),
                                count, (double)sleepInterval/1000D,
                                retryCause.getMessage(),
                                retryCause.getClass().getName());
                        logger.info(msg, e);
                    }
                    try
                    {
                        Thread.sleep(sleepInterval);
                    }
                    catch (InterruptedException ie)
                    {
                        // Do nothing.
                    }
                    StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.TX_RETRY, iterationStartTime);
                    // Try again
                    continue;
                }
                else
                {
                    // It was a 'bad' exception.
                    throw lastException;
                }
            }
        }
        // We've worn out our welcome and retried the maximum number of times.
        // So, fail.
        throw lastException;
    }

    private void checkNodesUpdated(Set<NodeRef> nodesUpdatedLocal) {
        String previousChangeTxnId = null;
        List<NodeRef> previousTxnChanges = null;
   
        for (NodeRef nodeRef : nodesUpdatedLocal) {
            String changeTxnId = null;
            ResultSet resultSet = BeanHelper.getSearchService().query(nodeRef.getStoreRef(), SearchService.LANGUAGE_LUCENE, "ID:\"" + nodeRef.toString() + "\"");
            try {
                for (int i = 0; i < resultSet.length(); i++) {
                    List<Fieldable> fields = ((LuceneResultSetRow) resultSet.getRow(i)).getDocument().getFields();
                    for (Fieldable field : fields) {
                        if ("TX".equals(field.name())) {
                            changeTxnId = field.stringValue();
                        }
                    }
                }
            } finally {
                resultSet.close();
            }
            if (changeTxnId == null) {
                logger.error("TransactionIntegrityChecker: No match found in lucene index for nodeRef = " + nodeRef);
                continue;
            }
            List<NodeRef> txnChanges;
            if (StringUtils.equals(changeTxnId, previousChangeTxnId)) {
                txnChanges = previousTxnChanges;
            } else {
                if (jdbcTemplate == null) {
                    jdbcTemplate = new SimpleJdbcTemplate(BeanHelper.getDataSource());
                }
                Long txnId = getTxnId(changeTxnId);
                if (txnId == null) {
                    logger.error("TransactionIntegrityChecker: No match found in alf_transaction for change_txn_id = " + changeTxnId + " ; nodeRef = " + nodeRef);
                    continue;
                }
                txnChanges = getNodeDaoService().getTxnChanges(txnId);
                previousTxnChanges = txnChanges;
                previousChangeTxnId = changeTxnId;
            }
            if (!txnChanges.contains(nodeRef)) {
                long realTxnId = jdbcTemplate.queryForLong("SELECT transaction_id FROM alf_node WHERE uuid = ?", nodeRef.getId());
                StringBuilder s = new StringBuilder("TransactionIntegrityChecker: nodeRef = " + nodeRef + " in lucene index has");
                logTxn(getTxnId(changeTxnId), s);
                s.append("\nbut in database it has");
                logTxn(realTxnId, s);
                s.append("\nand this thread is executing " + nodesUpdated.get().size() + " nested read-write transactions.");
                logger.error(s.toString());
            }
        }
    }

    private Long getTxnId(String changeTxnId) {
        try {
            return jdbcTemplate.queryForLong("SELECT id FROM alf_transaction WHERE change_txn_id = ?", changeTxnId);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    private void logTxn(long txnId, StringBuilder s) {
        Transaction dbTxn = getNodeDaoService().getTxnById(txnId);
        s.append("\n").append(CustomReindexComponent.getTransactionInfo(dbTxn)).append(" ").append(dbTxn.getChangeTxnId());
        List<NodeRef> txnChanges = getNodeDaoService().getTxnChanges(txnId);
        int txnDeleteCount = getNodeDaoService().getTxnDeleteCount(txnId);
        int txnUpdateCount = getNodeDaoService().getTxnUpdateCount(txnId);
        s.append("\n  updateCount=").append(txnUpdateCount).append(" deleteCount=").append(txnDeleteCount).append("\n  changes[").append(txnChanges.size()).append("]");
        for (NodeRef nodeRef : txnChanges) {
            s.append("\n    ").append(nodeRef.toString());
        }
    }

    /**
     * Sometimes, the exception means retry and sometimes not.
     *
     * @param cause     the cause to examine
     * @return          Returns the original cause if it is a valid retry cause, otherwise <tt>null</tt>
     */
    @SuppressWarnings("unchecked")
    public static Throwable extractRetryCause(Throwable cause)
    {
        Throwable retryCause = ExceptionStackUtil.getCause(cause, RETRY_EXCEPTIONS);
        
        if (retryCause == null)
        {
            return null;
        }
        else if (retryCause instanceof SQLGrammarException
                && ((SQLGrammarException) retryCause).getErrorCode() != 3960)
        {
           return null;
        }
        else if (retryCause instanceof NestedSQLException || retryCause instanceof UncategorizedSQLException)
        {
            // The exception will have been caused by something else, so check that instead
            if (retryCause.getCause() != null && retryCause.getCause() != retryCause)
            {
                // We dig further into this
                cause = retryCause.getCause();
                // Recurse
                return extractRetryCause(cause);
            }
            else
            {
                return null;
            }
        }
        // A simple match
        return retryCause;
    }
    
    /**
     * Utility method to get the active transaction.  The transaction status can be queried and
     * marked for rollback.
     * <p>
     * <b>NOTE:</b> Any attempt to actually commit or rollback the transaction will cause failures.
     * 
     * @return          Returns the currently active user transaction or <tt>null</tt> if
     *                  there isn't one.
     */
    public static UserTransaction getActiveUserTransaction()
    {
        // Dodge if there is no wrapping transaction
        if (AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_NONE)
        {
            return null;
        }
        // Get the current transaction.  There might not be one if the transaction was not started using
        // this class i.e. it wasn't started with retries.
        UserTransaction txn = (UserTransaction) AlfrescoTransactionSupport.getResource(KEY_ACTIVE_TRANSACTION);
        if (txn == null)
        {
            return null;
        }
        // Done
        return txn;
    }
    
    private static class UserTransactionProtectionAdvise implements MethodBeforeAdvice
    {
        public void before(Method method, Object[] args, Object target) throws Throwable
        {
            String methodName = method.getName();
            if (methodName.equals("begin") || methodName.equals("commit") || methodName.equals("rollback"))
            {
                throw new IllegalAccessException(
                        "The user transaction cannot be manipulated from within the transactional work load");
            }
        }
    }
}
