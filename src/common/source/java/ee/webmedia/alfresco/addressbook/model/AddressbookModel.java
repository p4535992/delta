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
        QName CONTACT_GROUP = QName.createQName(AddressbookModel.URI, "contactGroup");
        QName PERSON_BASE = QName.createQName(AddressbookModel.URI, "personBase");
    }

    interface Assocs {
        QName ADDRESSBOOK = QName.createQName(AddressbookModel.URI, "addressbook");
        QName ORGANIZATIONS = QName.createQName(AddressbookModel.URI, "organizations");
        QName ORGPEOPLE = QName.createQName(AddressbookModel.URI, "orgPeople");
        QName ABPEOPLE = QName.createQName(AddressbookModel.URI, "abPeople");
        QName CONTACT_GROUPS = QName.createQName(AddressbookModel.URI, "contactGroups");
        QName CONTACT_PERSON_BASE = QName.createQName(AddressbookModel.URI, "contactPersonBases");
        QName CONTACT_ORGANIZATION = QName.createQName(AddressbookModel.URI, "contactOrganizations");
    }

    interface Props {
        QName ACTIVESTATUS = QName.createQName(AddressbookModel.URI, "activeStatus");
        QName PERSON_FIRST_NAME = QName.createQName(AddressbookModel.URI, "personFirstName");
        QName PERSON_LAST_NAME = QName.createQName(AddressbookModel.URI, "personLastName");
        QName PERSON_ID = QName.createQName(AddressbookModel.URI, "personId");
        QName JOB_NAME = QName.createQName(AddressbookModel.URI, "jobName");
        QName PHONE = QName.createQName(AddressbookModel.URI, "phone");
        QName ORGANIZATION_NAME = QName.createQName(AddressbookModel.URI, "orgName");
        QName ORGANIZATION_ALTERNATE_NAME = QName.createQName(AddressbookModel.URI, "orgAltName");
        QName ORGANIZATION_ACRONYM = QName.createQName(AddressbookModel.URI, "orgAcronym");
        QName ORGANIZATION_CODE = QName.createQName(AddressbookModel.URI, "orgCode");
        QName DVK_CAPABLE = QName.createQName(AddressbookModel.URI, "dvkCapable");
        QName GROUP_NAME = QName.createQName(AddressbookModel.URI, "groupName");
        QName EMAIL = QName.createQName(AddressbookModel.URI, "email");
        QName FAX = QName.createQName(AddressbookModel.URI, "fax");
        QName POSTAL = QName.createQName(AddressbookModel.URI, "postal");
        QName ADDRESS1 = QName.createQName(AddressbookModel.URI, "address1");
        QName ADDRESS2 = QName.createQName(AddressbookModel.URI, "address2");
        QName CITY = QName.createQName(AddressbookModel.URI, "city");
        QName COUNTRY = QName.createQName(AddressbookModel.URI, "country");
    }
}
