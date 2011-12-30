package ee.webmedia.alfresco.user.bootstrap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Set all existing users' notification properties to TRUE (all users get all available e-mail notifications as default settings)
 * FIXME: NB! This updater MUST NOT run when migrating from 2.5 to 3.*!!!
 * 
 * @author Riina Tens
 */
public class NotificationPreferencesUpdater extends AbstractModuleComponent {
    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(NotificationPreferencesUpdater.class);

    @Override
    protected void executeInternal() throws Throwable {
        GeneralService generalService = BeanHelper.getGeneralService();
        if (!pathExists(generalService, "/sys:system") || !pathExists(generalService, "/sys:system/sys:people")) {
            LOG.debug("Users parent folder /sys:system/sys:people does not exist, skipping user preferences update.");
            return;
        }
        UserService userService = BeanHelper.getUserService();
        NotificationService notificationService = BeanHelper.getNotificationService();
        NodeService nodeService = BeanHelper.getNodeService();
        List<Node> users = userService.searchUsers(null, true, -1);
        List<QName> notificationProps = notificationService.getAllNotificationProps();
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();
        for (QName notificationProp : notificationProps) {
            updatedProps.put(notificationProp, Boolean.TRUE);
        }
        for (Node user : users) {
            NodeRef usersPreferenceNodeRef = userService.getUsersPreferenceNodeRef((String) user.getProperties().get(ContentModel.PROP_USERNAME));
            nodeService.addProperties(usersPreferenceNodeRef, updatedProps);
        }
    }

    private boolean pathExists(GeneralService generalService, String nodeRefXPath) {
        NodeRef parentRef = generalService.getNodeRef(nodeRefXPath);
        if (parentRef != null) {
            return true;
        }
        return false;
    }
}
