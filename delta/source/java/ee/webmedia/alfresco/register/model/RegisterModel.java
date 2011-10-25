package ee.webmedia.alfresco.register.model;

import org.alfresco.service.namespace.QName;

public interface RegisterModel {
    String URI = "http://alfresco.webmedia.ee/model/register/1.0";
    String NAMESPACE_PREFFIX = "reg:";

    interface Repo {
        final static String REGISTERS_PARENT = "/";
        final static String REGISTERS_SPACE = REGISTERS_PARENT + NAMESPACE_PREFFIX + Types.REGISTERS_ROOT.getLocalName();
    }

    interface Types {
        QName REGISTERS_ROOT = QName.createQName(URI, "registers");
        QName REGISTER = QName.createQName(URI, "register");
    }

    interface Prop {
        QName ID = QName.createQName(URI, "id");
        QName NAME = QName.createQName(URI, "name");
        QName COMMENT = QName.createQName(URI, "comment");
        QName ACTIVE = QName.createQName(URI, "active");
        QName AUTO_RESET = QName.createQName(URI, "autoReset");
        /** NB! not mappable - value stored in sequence */
        QName COUNTER = QName.createQName(URI, "counter");
    }

    interface Assoc {
        QName REGISTER = QName.createQName(URI, "register");
    }

}
