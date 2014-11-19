<<<<<<< HEAD
package ee.webmedia.alfresco.common.web;

import java.util.HashMap;
import java.util.Random;

/**
 * Session context.<br>
 * NB! Unsynchronized - should be accessed from single thread
 * 
 * @author Ats Uiboupin
 */
public class SessionContext extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "sessionContext";
    public static final Random r = new Random();

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value) {
        return add(value, null);
    }

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @param suggestedKeyPrefix - prefix to be used(suffix, will be generated)
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value, String suggestedKeyPrefix) {
        String key;
        do {
            key = suggestedKeyPrefix + "_" + Long.toString(Math.abs(r.nextLong()), 36);
        } while (containsKey(key));
        put(key, value);
        return key;
    }

}
=======
package ee.webmedia.alfresco.common.web;

import java.util.HashMap;
import java.util.Random;

/**
 * Session context.<br>
 * NB! Unsynchronized - should be accessed from single thread
 */
public class SessionContext extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "sessionContext";
    public static final Random r = new Random();

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value) {
        return add(value, null);
    }

    /**
     * Add new entry to sessionContext map(with given value - unique key is automatically generated and returned)
     * 
     * @param value
     * @param suggestedKeyPrefix - prefix to be used(suffix, will be generated)
     * @return unique key that can be used to access <code>value</code>
     */
    public String add(Object value, String suggestedKeyPrefix) {
        String key;
        do {
            key = suggestedKeyPrefix + "_" + Long.toString(Math.abs(r.nextLong()), 36);
        } while (containsKey(key));
        put(key, value);
        return key;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
