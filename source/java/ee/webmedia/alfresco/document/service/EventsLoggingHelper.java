<<<<<<< HEAD
package ee.webmedia.alfresco.document.service;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.RepoUtil;

public class EventsLoggingHelper {

    public static void disableLogging(Node docNode, String propIdentifier) {
        docNode.getProperties().put(propIdentifier, true);
    }

    public static void enableLogging(Node docNode, String propIdentifier) {
        docNode.getProperties().remove(propIdentifier);
    }

    public static boolean isLoggingDisabled(final Node docNode, String propIdentifier) {
        return RepoUtil.getPropertyBooleanValue(docNode.getProperties(), propIdentifier);
    }

=======
package ee.webmedia.alfresco.document.service;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.RepoUtil;

public class EventsLoggingHelper {

    public static void disableLogging(Node docNode, String propIdentifier) {
        docNode.getProperties().put(propIdentifier, true);
    }

    public static void enableLogging(Node docNode, String propIdentifier) {
        docNode.getProperties().remove(propIdentifier);
    }

    public static boolean isLoggingDisabled(final Node docNode, String propIdentifier) {
        return RepoUtil.getPropertyBooleanValue(docNode.getProperties(), propIdentifier);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}