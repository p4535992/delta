package ee.webmedia.alfresco.addressbook.service;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;


public class AddressbookEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;
    private NodeRef parentOrganizationRef;

    public AddressbookEntry(Node node) {
        this.node = node;
    }

    public AddressbookEntry(Node node, NodeRef parentOrganizationRef) {
        this.node = node;
        this.parentOrganizationRef = parentOrganizationRef;
    }

    public Node getNode() {
        return node;
    }

    public NodeRef getNodeRef() {
        return getNode().getNodeRef();
    }

    public String getName() {
        if (Types.ORGANIZATION.equals(node.getType())) {
            return getProp(AddressbookModel.Props.ORGANIZATION_NAME);
        } else if (Types.PRIV_PERSON.equals(node.getType()) || Types.ORGPERSON.equals(node.getType())) {
            return getProp(AddressbookModel.Props.PERSON_FIRST_NAME) + " " + getProp(AddressbookModel.Props.PERSON_LAST_NAME);
        } else if (Types.ORGCERTIFICATE.equals(node.getType())) {
            return getProp(AddressbookModel.Props.ORG_CERT_NAME);
        } else {
            return getProp(ContentModel.PROP_NAME);
        }
    }
    
    

    public NodeRef getParentOrgRef() {
        return parentOrganizationRef;
    }

    public boolean getActiveStatus() {
        return getProp(AddressbookModel.Props.ACTIVESTATUS);
    }

    public String getOrgName() {
        return getProp(AddressbookModel.Props.ORGANIZATION_NAME);
    }

    public String getOrgAcronym() {
        return getProp(AddressbookModel.Props.ORGANIZATION_ACRONYM);
    }

    public String getEmail() {
        return getProp(AddressbookModel.Props.EMAIL);
    }

    public Boolean getDvkCapable() {
        return getProp(AddressbookModel.Props.DVK_CAPABLE);
    }

    public String getPersonTitle() {
        return ""; //getProp(AddressbookModel.Props.???);
    }

    public String getPersonFirstName() {
        return getProp(AddressbookModel.Props.PERSON_FIRST_NAME);
    }

    public String getPersonLastName() {
        return getProp(AddressbookModel.Props.PERSON_LAST_NAME);
    }

    public String getPrivatePersonOrgName() {
        return getProp(AddressbookModel.Props.PRIVATE_PERSON_ORG_NAME);
    }

    public String getPersonId() {
        return getProp(AddressbookModel.Props.PERSON_ID);
    }
    
    public String getCertName() {
        return getProp(AddressbookModel.Props.ORG_CERT_NAME);
    }
    
    public Date getCertValidTo() {
        return getProp(AddressbookModel.Props.ORG_CERT_VALID_TO);
    }
    
    public String getCertDescription() {
        return getProp(AddressbookModel.Props.ORG_CERT_DESCRIPTION);
    }
    
    public void setCertDescription(String certDescription) {
        getNode().getProperties().put(AddressbookModel.Props.ORG_CERT_DESCRIPTION.toString(), certDescription);
    }
    
    public String getCertContent() {
        return getProp(AddressbookModel.Props.ORG_CERT_CONTENT);
    }

    private <T extends Serializable> T getProp(QName propName) {
        return (T) getNode().getProperties().get(propName);
    }
}
