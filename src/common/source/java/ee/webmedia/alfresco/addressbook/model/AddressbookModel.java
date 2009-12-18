package ee.webmedia.alfresco.addressbook.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Keit Tehvan
 */
public interface AddressbookModel {
    String URI = "http://alfresco.webmedia.ee/model/addressbook/1.0";

    interface Types {
        QName ORGANIZATION = QName.createQName(AddressbookModel.URI, "organization");
        QName ORGPERSON = QName.createQName(AddressbookModel.URI, "orgPerson");
        QName PRIV_PERSON = QName.createQName(AddressbookModel.URI, "privPerson");
    }

    interface Assocs {
        QName ADDRESSBOOK = QName.createQName(AddressbookModel.URI, "addressbook");
        QName ORGANIZATIONS = QName.createQName(AddressbookModel.URI, "organizations");
        QName ORGPEOPLE = QName.createQName(AddressbookModel.URI, "orgPeople");
        QName ABPEOPLE = QName.createQName(AddressbookModel.URI, "abPeople");
    }

    interface Props {
        QName PERSON_FIRST_NAME = QName.createQName(AddressbookModel.URI, "personFirstName");
        QName PERSON_LAST_NAME = QName.createQName(AddressbookModel.URI, "personLastName");
        QName ORGANIZATION_NAME = QName.createQName(AddressbookModel.URI, "orgName");
        QName EMAIL = QName.createQName(AddressbookModel.URI, "email");
    }
}
