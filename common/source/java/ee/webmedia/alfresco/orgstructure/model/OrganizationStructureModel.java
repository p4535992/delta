package ee.webmedia.alfresco.orgstructure.model;

import org.alfresco.service.namespace.QName;

public interface OrganizationStructureModel {
    String URI = "http://alfresco.webmedia.ee/model/orgstructure/1.0";
    String NAMESPACE_PREFFIX = "os:";

    public abstract class Repo {
        private static final String PARENT = "/";
        public static final String SPACE = PARENT + NAMESPACE_PREFFIX + Types.ORGSTRUCT_ROOT.getLocalName();
    }

    interface Types {
        QName ORGSTRUCT_ROOT = QName.createQName(URI, "orgstructs");
        QName ORGSTRUCT = QName.createQName(URI, "orgstruct");
    }

    interface Assocs {
        QName ORGSTRUCT = QName.createQName(URI, "orgstruct");

    }

    interface Props {
        QName NAME = QName.createQName(URI, "name");
        QName ORGANIZATION_PATH = QName.createQName(URI, "organizationPath");
        QName UNIT_ID = QName.createQName(URI, "unitId");
    }

}
