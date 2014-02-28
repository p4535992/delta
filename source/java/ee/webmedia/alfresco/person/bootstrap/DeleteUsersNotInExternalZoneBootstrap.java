package ee.webmedia.alfresco.person.bootstrap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Remove users that are not present in any configured AUTH.EXT.* zones. See CL task 195954 for details.
 * 
 * @author Riina Tens
 */
public class DeleteUsersNotInExternalZoneBootstrap extends AbstractNodeUpdater {

    private ChildApplicationContextManager applicationContextManager;
    private final Set<String> usersInExternalZones = new HashSet<String>();

    @Override
    protected void executeUpdater() throws Exception {
        AuthorityService authorityService = BeanHelper.getAuthorityService();
        for (String id : applicationContextManager.getInstanceIds()) {
            String zoneId = AuthorityService.ZONE_AUTH_EXT_PREFIX + id;
            usersInExternalZones.addAll(authorityService.getAllAuthoritiesInZone(zoneId, AuthorityType.USER));
        }
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("Method not used");
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        NodeRef peopleRef = BeanHelper.getPersonService().getPeopleContainer();
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(peopleRef);
        Set<NodeRef> personNodes = new HashSet<NodeRef>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            NodeRef nodeRef = ref.getChildRef();
            if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_PERSON)) {
                personNodes.add(nodeRef);
            }
        }
        return personNodes.isEmpty() ? null : personNodes;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!type.equals(ContentModel.TYPE_PERSON)) {
            return new String[] { "Not updating, node type is " + type };
        }
        String username = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME);
        if (usersInExternalZones.contains(username)) {
            return new String[] { "Not updating, user is present in AUTH.EXT.* zones " };
        }
        nodeService.deleteNode(nodeRef);
        return new String[] { "Deleting, user is NOT present in AUTH.EXT.* zones " };
    }

    public void setApplicationContextManager(ChildApplicationContextManager applicationContextManager) {
        this.applicationContextManager = applicationContextManager;
    }

}
