<<<<<<< HEAD
package ee.webmedia.alfresco.common.externalsession.service;

import java.util.Date;

import org.alfresco.util.Pair;

/**
 * @author Keit Tehvan
 */
public interface ExternalSessionService {

    String BEAN_NAME = "ExternalSessionService";

    Pair<String, Date> createSession(String username);

    String getUserForSession(String sessionId);

}
=======
package ee.webmedia.alfresco.common.externalsession.service;

import java.util.Date;

import org.alfresco.util.Pair;

public interface ExternalSessionService {

    String BEAN_NAME = "ExternalSessionService";

    Pair<String, Date> createSession(String username);

    String getUserForSession(String sessionId);

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
