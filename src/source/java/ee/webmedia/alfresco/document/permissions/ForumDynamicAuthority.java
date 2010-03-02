package ee.webmedia.alfresco.document.permissions;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class ForumDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ForumDynamicAuthority.class);

    public static final String FORUM_AUTHORITY = "ROLE_FORUM";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (type.equals(ForumModel.TYPE_FORUM) || type.equals(ForumModel.TYPE_FORUMS) || type.equals(ForumModel.TYPE_TOPIC) || type.equals(ForumModel.TYPE_POST)) {
            log.debug("Node type is" + type + ", granting authority " + getAuthority());
            return true;
        }
        log.trace("Node is not related to forums, type=" + type + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return FORUM_AUTHORITY;
    }

}
