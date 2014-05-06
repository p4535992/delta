package ee.webmedia.alfresco.document.forum.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.forum.web.InviteUsersDialog;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.Privilege;

/**
 * To allow optimization of counting and listing documents that have forums where given user is invited following was done:
 * When adding user/group to forum, it is added to the document property {@link DocumentCommonModel.Props#FORUM_PARTICIPANTS} as well.
 */
public class ForumPermissionRefactorUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ForumModel.TYPE_FORUM);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query)
                , searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query)
                );
    }

    @Override
    protected String[] updateNode(NodeRef forumRef) throws Exception {
        List<String> addedAuthorities = BeanHelper.getPrivilegeService().getAuthoritiesWithPrivilege(forumRef, Privilege.PARTICIPATE_AT_FORUM);
        InviteUsersDialog.updateDocument(addedAuthorities, forumRef);
        return new String[] { StringUtils.join(addedAuthorities, ", ") };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "forumNodeRef", "participants at forum" };
    }

}
