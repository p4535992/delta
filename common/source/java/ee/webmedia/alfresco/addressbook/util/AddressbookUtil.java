package ee.webmedia.alfresco.addressbook.util;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.UserContactGroupSearchBean.FILTER_INDEX_SEPARATOR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Props;
import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Types;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class AddressbookUtil {

    public static String getContactFullName(Map<QName, Serializable> props, QName contactType) {
        String name;
        if (Types.CONTACT_GROUP.equals(contactType)) {
            name = (String) props.get(AddressbookModel.Props.GROUP_NAME);
        } else if (Types.ORGANIZATION.equals(contactType)) {
            name = (String) props.get(AddressbookModel.Props.ORGANIZATION_NAME);
        } else {
            name = UserUtil.getPersonFullName((String) props.get(AddressbookModel.Props.PERSON_FIRST_NAME), (String) props
                    .get(AddressbookModel.Props.PERSON_LAST_NAME));
        }
        return name;
    }

    public static String getContactFullName(NodeRef contactRef) {
        return getContactFullName(BeanHelper.getNodeService().getProperties(contactRef), BeanHelper.getNodeService().getType(contactRef));
    }

    public static List<String> getContactData(String nodeRef) {
        List<String> list = new ArrayList<String>();
        Map<QName, Serializable> props = getNodeService().getProperties(new NodeRef(nodeRef));

        QName type = getNodeService().getType(new NodeRef(nodeRef));
        String name = getContactFullName(props, type);

        list.add(name);
        list.add((String) props.get(AddressbookModel.Props.PERSON_ID));
        list.add((String) props.get(AddressbookModel.Props.EMAIL));
        return list;
    }

    private static String createSelectItemDescription(Node addressBookNode) {
        Map<String, Object> props = addressBookNode.getProperties();
        StringBuilder description = new StringBuilder();
        if (addressBookNode.getType().equals(Types.PRIV_PERSON)) {
            description.append(UserUtil.getPersonFullName((String) props.get(Props.PERSON_FIRST_NAME), (String) props.get(Props.PERSON_LAST_NAME)));
        } else if (addressBookNode.getType().equals(Types.ORGANIZATION)) {
            description.append((String) props.get(Props.ORGANIZATION_NAME.toString()));
        } else {
            return null;
        }
        List<String> descriptionList = new ArrayList<String>();
        for (QName propName : Arrays.asList(Props.ADDRESS1, Props.ADDRESS2, Props.POSTAL, Props.CITY)) {
            String propValue = (String) props.get(propName.toString());
            if (StringUtils.isNotBlank(propValue)) {
                descriptionList.add(propValue);
            }
        }
        if (!descriptionList.isEmpty()) {
            description.append(" (" + StringUtils.join(descriptionList, ", ") + ")");
        }
        return description.toString();
    }

    /**
     * Transforms the list of contact nodes (usually returned by the search) to SelectItems in the following form:
     * OrganizationName (organizationLabel, email) -- if it's an organization
     * FirstName LastName (privPersonLabel, email) -- if it's a person (can be both orgPerson or privPerson in the model)
     * 
     * @param nodes
     * @param privPersonLabel
     * @param organizationLabel
     * @return
     */
    public static SelectItem[] transformAddressbookNodesToSelectItems(List<Node> nodes) {
        return transformAddressbookNodesToSelectItems(nodes, null);
    }

    public static SelectItem[] transformAddressbookNodesToSelectItems(List<Node> nodes, Integer filter) {
        SelectItem[] results = new SelectItem[nodes.size()];
        int i = 0;
        for (Node node : nodes) {
            String value = node.getNodeRefAsString();
            StringBuilder label = new StringBuilder();
            Map<String, Object> props = node.getProperties();
            boolean isOrgPerson = node.getType().equals(Types.ORGPERSON);
            if (node.getType().equals(Types.ORGANIZATION)) {
                String orgName = (String) props.get(Props.ORGANIZATION_NAME.toString());
                label.append(orgName);
                label.append(" (");
                label.append(MessageUtil.getMessage("addressbook_org").toLowerCase());
            } else if (node.getType().equals(Types.CONTACT_GROUP)) {
                label.append((String) props.get(Props.GROUP_NAME));
            } else {
                String personName = UserUtil.getPersonFullName((String) props.get(Props.PERSON_FIRST_NAME.toString()), (String) props
                        .get(Props.PERSON_LAST_NAME.toString()));
                label.append(personName);
                label.append(" (");
                label.append(MessageUtil.getMessage(isOrgPerson ? "addressbook_contactperson" : "addressbook_private_person").toLowerCase());
            }
            String afterComma;
            if (isOrgPerson) {
                afterComma = (String) props.get(Props.PRIVATE_PERSON_ORG_NAME.toString());
            } else {
                afterComma = (String) props.get(Props.EMAIL.toString());
            }
            if (StringUtils.isNotEmpty(afterComma)) {
                label.append(", ");
                label.append(afterComma);
            }
            if (!node.getType().equals(Types.CONTACT_GROUP)) {
                label.append(")");
            }
            String description = createSelectItemDescription(node);
            if (filter != null) {
                value += (FILTER_INDEX_SEPARATOR + filter);
            }
            if (StringUtils.isNotBlank(description)) {
                results[i++] = new SelectItem(value, label.toString(), description);
            } else {
                results[i++] = new SelectItem(value, label.toString());
            }
        }

        WebUtil.sort(results);
        return results;
    }

    public static NodePropertyResolver resolverParentOrgName = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            return getNodeService().getProperty(getAddressbookService().getOrgOfPerson(node.getNodeRef()), AddressbookModel.Props.ORGANIZATION_NAME);
        }
    };

    public static NodePropertyResolver resolverParentOrgRef = new NodePropertyResolver() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(Node node) {
            return getAddressbookService().getOrgOfPerson(node.getNodeRef());
        }
    };

}
