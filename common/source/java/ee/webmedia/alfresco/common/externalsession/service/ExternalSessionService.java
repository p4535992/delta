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
>>>>>>> develop-5.1
