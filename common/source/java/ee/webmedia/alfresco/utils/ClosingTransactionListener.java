package ee.webmedia.alfresco.utils;

import java.io.Closeable;
import java.io.IOException;

import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.springframework.util.Assert;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class ClosingTransactionListener extends TransactionListenerAdapter {

    private final Closeable closeable;

    public ClosingTransactionListener(Closeable closeable) {
        Assert.notNull(closeable);
        this.closeable = closeable;
    }

    @Override
    public void afterCommit() {
        try {
            closeable.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Override
    public void afterRollback() {
        try {
            closeable.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

}
