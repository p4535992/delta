package ee.webmedia.alfresco.addressbook.service;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.addressbook.util.AddressbookUtil;

/**
 * @author Priit Pikk
 */

public class AddressbookEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;

    public AddressbookEntry(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public String getName() {
        if (Types.ORGANIZATION.equals(node.getType())) {
            return (String) node.getProperties().get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else if (Types.PRIV_PERSON.equals(node.getType()) || Types.ORGPERSON.equals(node.getType())) {
            return (String) node.getProperties().get(AddressbookModel.Props.PERSON_FIRST_NAME) + " " + (String) node.getProperties().get(AddressbookModel.Props.PERSON_LAST_NAME);
        } else {
            return (String) node.getProperties().get(ContentModel.PROP_NAME);
        }
    }

    public String getParentOrgName() {
        return (String) AddressbookUtil.resolverParentOrgName.get(node);
    }
}