package ee.webmedia.alfresco.substitute.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.substitute.model.SubstituteModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * After merging Delta 2012-2013 data to Delta 2014 data, there may occur duplicate substitute root nodes under person nodes.
 * Seems that the reason for it is that substitute roots have been deleted and recreated under Delta 2014,
 * which does not follow the assumption that those root nodes (in case they exist) have same value.
 * This updater repairs the situation, moving all substitutes under one root and deleting another root.
 */
public class RepairSubstitutesAfterMerge extends AbstractNodeUpdater {
    private static final Log LOG = LogFactory.getLog(RepairSubstitutesAfterMerge.class);

    private static final Set<QName> SUBSTITUTES_ROOT_QNAME = new HashSet<>(Arrays.asList(SubstituteModel.Types.SUBSTITUTES));

    private BulkLoadNodeService bulkLoadNodeService;

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        LOG.info("Starting to repair double substitutes roots.");
        UserService userService = BeanHelper.getUserService();
        Set<String> usernames = userService.getAllUsersUsernames();
        if (usernames.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set<NodeRef> resultSet = new HashSet<>();
        for (String username : usernames) {
            NodeRef userRef = BeanHelper.getPersonService().getPerson(username);
            if (userRef != null) {
                resultSet.add(userRef);
            }
        }
        return resultSet;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        throw new RuntimeException("Method not implemented!");
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<ChildAssociationRef> substituteRoots = nodeService.getChildAssocs(nodeRef, SUBSTITUTES_ROOT_QNAME);
        if (substituteRoots.size() < 2) {
            return new String[] { "No duplicate roots present" };
        }
        NodeRef newParentRef = substituteRoots.get(0).getChildRef();
        NodeRef rootToDelete = substituteRoots.get(1).getChildRef();
        List<NodeRef> substitutesToMove = bulkLoadNodeService.loadChildRefs(rootToDelete, SubstituteModel.Types.SUBSTITUTE);
        for (NodeRef substituteToMove : substitutesToMove) {
            nodeService.moveNode(substituteToMove, newParentRef, SubstituteModel.Associations.SUBSTITUTE, SubstituteModel.Associations.SUBSTITUTE);
        }
        nodeService.deleteNode(rootToDelete);
        return new String[] { "Moved " + substitutesToMove.size() + " substitutes." };
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
