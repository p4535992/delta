<<<<<<< HEAD
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
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
    public boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Starting RemoveIdenticalContacts updater.");

        LOG.info("Starting to load organizations.");
        List<Node> organizations = addressbookService.listOrganization();
        LOG.info("Found " + organizations.size() + " organization contacts, sorting list.");
        removeNodeRefProps(organizations);

        Map<String, List<Node>> organizationsWithOrgCode = new HashMap<String, List<Node>>();
        List<Node> organizationsWithoutOrgCode = new ArrayList<Node>();
        for (Node organization : organizations) {
            String orgCode = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
            if (StringUtils.isNotBlank(orgCode)) {
                if (!organizationsWithOrgCode.containsKey(orgCode)) {
                    organizationsWithOrgCode.put(orgCode, new ArrayList<Node>());
                }
                List<Node> sameOrgCodeOrgs = organizationsWithOrgCode.get(orgCode);
                sameOrgCodeOrgs.add(organization);
            } else {
                organizationsWithoutOrgCode.add(organization);
            }
        }

        final Set<NodeRef> nodesToRemove = new HashSet<NodeRef>();

        LOG.info("Identifying duplicate organizations.");
        for (List<Node> sameOrgCodeOrgs : organizationsWithOrgCode.values()) {
            collectDuplicateValues(nodesToRemove, sameOrgCodeOrgs);
        }

        collectDuplicateValues(nodesToRemove, organizationsWithoutOrgCode);

        LOG.info("Starting to load persons.");
        List<Node> persons = addressbookService.listPerson();
        LOG.info("Found " + persons.size() + " person contacts, searching for duplicate entries.");
        removeNodeRefProps(persons);
        LOG.info("Identifying duplicate persons.");
        for (Node person : persons) {
            Map<String, Object> orgProps = person.getProperties();
            NodeRef personNodeRef = person.getNodeRef();
            for (Node comparePerson : persons) {
                if (!personNodeRef.equals(comparePerson.getNodeRef()) && !nodesToRemove.contains(personNodeRef)
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

    private void collectDuplicateValues(final Set<NodeRef> nodesToRemove, List<Node> sameOrgCodeOrgs) {
        for (Node organization : sameOrgCodeOrgs) {
            for (Node compareOrg : sameOrgCodeOrgs) {
                NodeRef orgNodeRef = organization.getNodeRef();
                if (!orgNodeRef.equals(compareOrg.getNodeRef()) && !nodesToRemove.contains(orgNodeRef)
                        && RepoUtil.propsEqual(organization.getProperties(), compareOrg.getProperties())) {
                    nodesToRemove.add(compareOrg.getNodeRef());
                }
            }
        }
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
=======
package ee.webmedia.alfresco.addressbook.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Remove identical contacts (persons and organizations). Contacts are considered identical when all non-system properties have identical values.
 */
public class RemoveIdenticalContacts extends AbstractModuleComponent {

    private static final QName RESIDUAL_PROP_NODE_REF = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nodeRef");
    private static final QName RESIDUAL_PROP_ID = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "id");
    private static final QName RESIDUAL_PROP_NODE_REF_AS_STRING = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nodeRefAsString");

    protected final Log LOG = LogFactory.getLog(getClass());
    private AddressbookService addressbookService;
    private NodeService nodeService;

    @Override
    public boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Starting RemoveIdenticalContacts updater.");

        LOG.info("Starting to load organizations.");
        List<Node> organizations = addressbookService.listOrganization();
        LOG.info("Found " + organizations.size() + " organization contacts, sorting list.");
        removeNodeRefProps(organizations);

        Map<String, List<Node>> organizationsWithOrgCode = new HashMap<String, List<Node>>();
        List<Node> organizationsWithoutOrgCode = new ArrayList<Node>();
        for (Node organization : organizations) {
            String orgCode = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE);
            if (StringUtils.isNotBlank(orgCode)) {
                if (!organizationsWithOrgCode.containsKey(orgCode)) {
                    organizationsWithOrgCode.put(orgCode, new ArrayList<Node>());
                }
                List<Node> sameOrgCodeOrgs = organizationsWithOrgCode.get(orgCode);
                sameOrgCodeOrgs.add(organization);
            } else {
                organizationsWithoutOrgCode.add(organization);
            }
        }

        final Set<NodeRef> nodesToRemove = new HashSet<NodeRef>();

        LOG.info("Identifying duplicate organizations.");
        for (List<Node> sameOrgCodeOrgs : organizationsWithOrgCode.values()) {
            collectDuplicateValues(nodesToRemove, sameOrgCodeOrgs);
        }

        collectDuplicateValues(nodesToRemove, organizationsWithoutOrgCode);

        LOG.info("Starting to load persons.");
        List<Node> persons = addressbookService.listPerson();
        LOG.info("Found " + persons.size() + " person contacts, searching for duplicate entries.");
        removeNodeRefProps(persons);
        LOG.info("Identifying duplicate persons.");
        for (Node person : persons) {
            Map<String, Object> orgProps = person.getProperties();
            NodeRef personNodeRef = person.getNodeRef();
            for (Node comparePerson : persons) {
                if (!personNodeRef.equals(comparePerson.getNodeRef()) && !nodesToRemove.contains(personNodeRef)
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

    private void collectDuplicateValues(final Set<NodeRef> nodesToRemove, List<Node> sameOrgCodeOrgs) {
        for (Node organization : sameOrgCodeOrgs) {
            for (Node compareOrg : sameOrgCodeOrgs) {
                NodeRef orgNodeRef = organization.getNodeRef();
                if (!orgNodeRef.equals(compareOrg.getNodeRef()) && !nodesToRemove.contains(orgNodeRef)
                        && RepoUtil.propsEqual(organization.getProperties(), compareOrg.getProperties())) {
                    nodesToRemove.add(compareOrg.getNodeRef());
                }
            }
        }
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
