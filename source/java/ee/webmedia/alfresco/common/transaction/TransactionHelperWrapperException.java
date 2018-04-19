package ee.webmedia.alfresco.common.transaction;

/**
 * Wrapper to throw runtime exception from RetryingTransactionHelper
 */
public class TransactionHelperWrapperException extends RuntimeException {

    public TransactionHelperWrapperException(Throwable e) {
        super(e);
    }

}
