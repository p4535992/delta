package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Remove identical contacts (persons and organizations). Contacts are considered identical when all non-system properties have identical values.
 * 
 * @author Riina Tens
 */
public class RemoveIdenticalContacts extends AbstractModuleComponent {

    private static final QName RESIDUAL_PROP_NODE_REF = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nodeRef");
    private static final QName RESIDUAL_PROP_ID = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "id");
    private static final QName RESIDUAL_PROP_NODE_REF_AS_STRING = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nodeRefAsString");

    protected final Log LOG = LogFactory.getLog(getClass());
    private AddressbookService addressbookService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Starting RemoveIdenticalContacts updater.");
        @SuppressWarnings("unchecked")
        Comparator<Node> comparator = new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object arg0) {
                return ((Node) arg0).getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
            }

        }, new NullComparator());
        LOG.info("Starting to load organizations.");
        List<Node> organizations = addressbookService.listOrganization();
        LOG.info("Found " + organizations.size() + " organization contacts, sorting list.");
        Collections.sort(organizations, comparator);
        removeNodeRefProps(organizations);

        final Set<NodeRef> nodesToRemove = new HashSet<NodeRef>();
        Node previousOrg = null;

        LOG.info("Identifying duplicate organizations.");
        for (Node organization : organizations) {
            if (previousOrg == null) {
                previousOrg = organization;
                continue;
            }
            Map<String, Object> orgProps = organization.getProperties();
            QName compareProp = AddressbookModel.Props.ORGANIZATION_CODE;
            Map<String, Object> previousOrgProps = previousOrg.getProperties();
            if (StringUtils.isNotBlank((String) previousOrgProps.get(compareProp))
                    && StringUtils.isNotBlank((String) orgProps.get(compareProp))) {
                // organizations in sorted list with non-null organization code are compared only to previous node
                if (!organization.getNodeRef().equals(previousOrg.getNodeRef()) && !nodesToRemove.contains(organization.getNodeRef())
                        && RepoUtil.propsEqual(orgProps, previousOrgProps)) {
                    nodesToRemove.add(organization.getNodeRef());
                    continue;
                }
            } else {
                // nodes with null organization code are compared to all other nodes
                for (Node compareOrg : organizations) {
                    if (!organization.getNodeRef().equals(compareOrg.getNodeRef()) && !nodesToRemove.contains(compareOrg.getNodeRef())
                            && RepoUtil.propsEqual(orgProps, compareOrg.getProperties())) {
                        nodesToRemove.add(organization.getNodeRef());
                        continue;
                    }
                }
            }
            previousOrg = organization;
        }

        LOG.info("Starting to load persons.");
        List<Node> persons = addressbookService.listPerson();
        LOG.info("Found " + persons.size() + " person contacts, searching for duplicate entries.");
        removeNodeRefProps(persons);
        LOG.info("Identifying duplicate persons.");
        for (Node person : persons) {
            Map<String, Object> orgProps = person.getProperties();
            NodeRef personNodeRef = person.getNodeRef();
            for (Node comparePerson : persons) {
                if (!personNodeRef.equals(comparePerson.getNodeRef()) && !nodesToRemove.contains(comparePerson.getNodeRef())
                        && RepoUtil.propsEqual(orgProps, comparePerson.getProperties())) {
                    nodesToRemove.add(personNodeRef);
                }
            }
        }

        LOG.info("Starting to remove " + nodesToRemove.size() + " duplicate contacts.");
        final Iterator<NodeRef> it = nodesToRemove.iterator();
        while (it.hasNext()) {
            BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    int j = 0;
                    while (it.hasNext() && j++ < 25) {
                        NodeRef nodeToRemove = it.next();
                        nodeService.deleteNode(nodeToRemove);
                        it.remove();
                    }
                    LOG.info("Removed " + j + " contacts, " + nodesToRemove.size() + " left to delete");
                    return null;
                }
            }, false, true);
        }
        LOG.info("Removing duplicate contacts completed.");
    }

    // somehow some organization/contact nodes have non-model residual properties, ignore them
    private void removeNodeRefProps(List<Node> contacts) {
        for (Node contact : contacts) {
            Map<String, Object> contactProps = contact.getProperties();
            contactProps.remove(RESIDUAL_PROP_NODE_REF);
            contactProps.remove(RESIDUAL_PROP_ID);
            contactProps.remove(RESIDUAL_PROP_NODE_REF_AS_STRING);
        }
    }

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
