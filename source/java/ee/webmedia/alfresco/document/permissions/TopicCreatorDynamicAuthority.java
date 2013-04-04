package ee.webmedia.alfresco.document.permissions;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class TopicCreatorDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TopicCreatorDynamicAuthority.class);

    public static final String TOPIC_CREATOR_AUTHORITY = "ROLE_TOPIC_CREATOR";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!type.equals(ForumModel.TYPE_TOPIC)) {
            log.trace("Node is not forum topic, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        String creator = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);
        if (userName.equals(creator)) {
            log.debug("Node type=" + type + ", userName=" + userName + " is creator, granting authority " + getAuthority());
            return true;
        }
        log.trace("Node type=" + type + ", but current user is not creator, refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return TOPIC_CREATOR_AUTHORITY;
    }

}
