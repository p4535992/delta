<<<<<<< HEAD
package ee.webmedia.alfresco.document.forum.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.forum.web.InviteUsersDialog;
import ee.webmedia.alfresco.document.forum.web.evaluator.DiscussNodeEvaluator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Initially management of users invited to the forum was done using {@link ForumPermissionRefactorUpdater#OLD_PARTICIPATE_AT_FORUM} permission.
 * Since it was misleading it was renamed to {@link DiscussNodeEvaluator#PARTICIPATE_AT_FORUM}.<br>
 * <br>
 * To allow optimization of counting and listing documents that have forums where given user is invited following was done:
 * When adding user/group to forum, it is added to the document property {@link DocumentCommonModel.Props#FORUM_PARTICIPANTS} as well.
 * 
 * @author Ats Uiboupin
 */
public class ForumPermissionRefactorUpdater extends AbstractNodeUpdater {
    private static final String OLD_PARTICIPATE_AT_FORUM = "DocumentFileRead";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ForumModel.TYPE_FORUM);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected String[] updateNode(NodeRef forumRef) throws Exception {
        List<String> addedAuthorities = new ArrayList<String>();
        PermissionService permissionService = getPermissionService();
        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(forumRef)) {
            if (OLD_PARTICIPATE_AT_FORUM.equals(accessPermission.getPermission()) && accessPermission.isSetDirectly()) {
                String authority = accessPermission.getAuthority();
                permissionService.deletePermission(forumRef, authority, OLD_PARTICIPATE_AT_FORUM);
                permissionService.setPermission(forumRef, authority, DiscussNodeEvaluator.PARTICIPATE_AT_FORUM, true);
                addedAuthorities.add(authority);
            }
        }
        InviteUsersDialog.updateDocument(addedAuthorities, forumRef);
        return new String[] { StringUtils.join(addedAuthorities, ", ") };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "forumNodeRef", "participants at forum" };
    }

}
=======
package ee.webmedia.alfresco.document.forum.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.forum.web.InviteUsersDialog;
import ee.webmedia.alfresco.document.forum.web.evaluator.DiscussNodeEvaluator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Initially management of users invited to the forum was done using {@link ForumPermissionRefactorUpdater#OLD_PARTICIPATE_AT_FORUM} permission.
 * Since it was misleading it was renamed to {@link DiscussNodeEvaluator#PARTICIPATE_AT_FORUM}.<br>
 * <br>
 * To allow optimization of counting and listing documents that have forums where given user is invited following was done:
 * When adding user/group to forum, it is added to the document property {@link DocumentCommonModel.Props#FORUM_PARTICIPANTS} as well.
 */
public class ForumPermissionRefactorUpdater extends AbstractNodeUpdater {
    private static final String OLD_PARTICIPATE_AT_FORUM = "DocumentFileRead";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ForumModel.TYPE_FORUM);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected String[] updateNode(NodeRef forumRef) throws Exception {
        List<String> addedAuthorities = new ArrayList<String>();
        PermissionService permissionService = getPermissionService();
        for (AccessPermission accessPermission : permissionService.getAllSetPermissions(forumRef)) {
            if (OLD_PARTICIPATE_AT_FORUM.equals(accessPermission.getPermission()) && accessPermission.isSetDirectly()) {
                String authority = accessPermission.getAuthority();
                permissionService.deletePermission(forumRef, authority, OLD_PARTICIPATE_AT_FORUM);
                permissionService.setPermission(forumRef, authority, DiscussNodeEvaluator.PARTICIPATE_AT_FORUM, true);
                addedAuthorities.add(authority);
            }
        }
        InviteUsersDialog.updateDocument(addedAuthorities, forumRef);
        return new String[] { StringUtils.join(addedAuthorities, ", ") };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "forumNodeRef", "participants at forum" };
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
