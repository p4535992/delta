package org.alfresco.repo.transaction;

import org.alfresco.service.transaction.TransactionService;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

/**
 * TransactionManager to access RetryingTransactionHelper via transactionService
 */
public class RetryingTransactionManager extends HibernateTransactionManager {

    private static final long serialVersionUID = 1L;

    private TransactionService transactionService;

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (transactionService == null) {
            throw new IllegalArgumentException("Property 'transactionService' is required");
        }
    }

    public boolean isExistingTransaction() {
        Object transaction = doGetTransaction();
        return isExistingTransaction(transaction);
    }

    public RetryingTransactionHelper getRetryingTransactionHelper() {
        return transactionService.getRetryingTransactionHelper();
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

}
